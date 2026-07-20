package africa.matchpass.sdk.internal

import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.ContentType
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AccessCheckerTest {

    private val config = MatchPassConfig(apiKey = "test-key")
    private val service = mockk<MatchPassService>()
    private val store = mockk<MatchPassStore>(relaxed = true)

    private val content = MatchPassContent(
        id = "epl-match-001",
        title = "Arsenal vs Manchester City",
        price = "29.00",
        contentType = ContentType.MATCH,
    )

    private lateinit var checker: AccessChecker

    @Before
    fun setUp() {
        checker = AccessChecker(config, service, store)
    }

    // ── No token stored ───────────────────────────────────────────────────────

    @Test
    fun `returns NotPurchased when no token is stored`() = runTest {
        every { store.getToken(content.id) } returns null

        val result = checker.check(content)

        assertTrue(result is AccessResult.NotPurchased)
        coVerify(exactly = 0) { service.validatePass(any(), any()) }
    }

    // ── Local expiry detection ────────────────────────────────────────────────

    @Test
    fun `returns Expired immediately when stored expiry is in the past without server call`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getExpiresAt(content.id) } returns System.currentTimeMillis() - 5_000L // 5s ago

        val result = checker.check(content)

        assertTrue(result is AccessResult.Expired)
        coVerify(exactly = 0) { service.validatePass(any(), any()) }
    }

    @Test
    fun `clears pass when locally detected as expired`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getExpiresAt(content.id) } returns System.currentTimeMillis() - 1L

        checker.check(content)

        verify { store.clearPass(content.id) }
    }

    // ── Cache hit — skip server ────────────────────────────────────────────────

    @Test
    fun `returns Granted from cache when within TTL without server call`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getExpiresAt(content.id) } returns 0L  // no expiry stored
        every { store.getValidationTime(content.id) } returns System.currentTimeMillis() - 1_000L // 1s ago

        val result = checker.check(content)

        assertTrue(result is AccessResult.Granted)
        assertEquals("tok-123", (result as AccessResult.Granted).grant.token)
        coVerify(exactly = 0) { service.validatePass(any(), any()) }
    }

    // ── Cache miss — validate with server ────────────────────────────────────

    @Test
    fun `calls server when cache timestamp is zero`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass("ApiKey test-key", "tok-123") } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-01-01T00:00:00Z")

        val result = checker.check(content)

        assertTrue(result is AccessResult.Granted)
        coVerify(exactly = 1) { service.validatePass("ApiKey test-key", "tok-123") }
    }

    @Test
    fun `calls server when cache TTL has elapsed`() = runTest {
        val expiredTimestamp = System.currentTimeMillis() - (content.policy.cacheTtlSeconds + 60) * 1_000L
        every { store.getToken(content.id) } returns "tok-abc"
        every { store.getValidationTime(content.id) } returns expiredTimestamp
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-01-01T00:00:00Z")

        checker.check(content)

        coVerify(exactly = 1) { service.validatePass(any(), "tok-abc") }
    }

    @Test
    fun `saves new validation timestamp on successful server check`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-01-01T00:00:00Z")

        checker.check(content)

        verify { store.saveValidationTime(content.id, any()) }
    }

    // ── Lifetime pass (null expires_at) ──────────────────────────────────────
    // Movies/series are owned for life — the server sends expires_at: null.
    // Regression test: previously ValidatePassDto.expiresAt was a non-null
    // String, so Gson setting an actual null into it crashed here.

    @Test
    fun `grants access for a lifetime pass with null expiry from server`() = runTest {
        every { store.getToken(content.id) } returns "tok-lifetime"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = null)

        val result = checker.check(content)

        assertTrue(result is AccessResult.Granted)
        assertEquals(null, (result as AccessResult.Granted).grant.expiresAt)
        verify(exactly = 0) { store.saveExpiresAt(content.id, any()) }
    }

    @Test
    fun `saves expiry epoch millis from server response`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-06-15T12:00:00.000000Z")

        checker.check(content)

        verify { store.saveExpiresAt(content.id, any()) }
    }

    // ── parseIso8601ToMillis ──────────────────────────────────────────────────

    @Test
    fun `parseIso8601ToMillis parses seconds-only UTC format`() {
        val result = AccessChecker.parseIso8601ToMillis("2099-01-01T00:00:00Z")
        assertTrue(result != null && result > 0)
    }

    @Test
    fun `parseIso8601ToMillis parses DRF microseconds format`() {
        val result = AccessChecker.parseIso8601ToMillis("2099-01-01T00:00:00.123456Z")
        assertTrue(result != null && result > 0)
    }

    @Test
    fun `parseIso8601ToMillis returns null for blank string`() {
        assertFalse(AccessChecker.parseIso8601ToMillis("") != null)
    }

    @Test
    fun `returns Expired and clears store when server says invalid with expired status`() = runTest {
        every { store.getToken(content.id) } returns "tok-old"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = false, status = "expired", expiresAt = "2020-01-01T00:00:00Z")

        val result = checker.check(content)

        assertTrue(result is AccessResult.Expired)
        assertEquals("2020-01-01T00:00:00Z", (result as AccessResult.Expired).expiredAt)
        verify { store.clearPass(content.id) }
    }

    @Test
    fun `returns Error with PassRevoked when server says revoked`() = runTest {
        every { store.getToken(content.id) } returns "tok-revoked"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = false, status = "revoked", expiresAt = "")

        val result = checker.check(content)

        assertTrue(result is AccessResult.Error)
        assertTrue((result as AccessResult.Error).exception is MatchPassException.PassRevoked)
        verify { store.clearPass(content.id) }
    }

    // ── Network / server errors ────────────────────────────────────────────────

    @Test
    fun `returns Error with NetworkError on IOException without clearing pass`() = runTest {
        every { store.getToken(content.id) } returns "tok-123"
        every { store.getValidationTime(content.id) } returns 0L
        coEvery { service.validatePass(any(), any()) } throws IOException("timeout")

        val result = checker.check(content)

        assertTrue(result is AccessResult.Error)
        assertTrue((result as AccessResult.Error).exception is MatchPassException.NetworkError)
        verify(exactly = 0) { store.clearPass(any()) }
    }

    // ── Long-TTL content types ─────────────────────────────────────────────────

    @Test
    fun `MOVIE content with fresh cache never hits server even after an hour`() = runTest {
        val movieContent = MatchPassContent(
            id = "movie-001", title = "The Woman King", price = "35.00",
            contentType = ContentType.MOVIE,
        )
        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1_000L
        every { store.getToken(movieContent.id) } returns "tok-movie"
        every { store.getValidationTime(movieContent.id) } returns oneHourAgo

        val result = checker.check(movieContent)

        assertTrue(result is AccessResult.Granted)
        coVerify(exactly = 0) { service.validatePass(any(), any()) }
    }

    @Test
    fun `CHANNEL content with 90s old cache calls server`() = runTest {
        val channelContent = MatchPassContent(
            id = "supersport1", title = "SuperSport 1", price = "15.00",
            contentType = ContentType.CHANNEL,
        )
        val ninetySecondsAgo = System.currentTimeMillis() - 90 * 1_000L
        every { store.getToken(channelContent.id) } returns "tok-ch"
        every { store.getValidationTime(channelContent.id) } returns ninetySecondsAgo
        coEvery { service.validatePass(any(), any()) } returns
            ValidatePassDto(isValid = true, status = "active", expiresAt = "2099-01-01T00:00:00Z")

        checker.check(channelContent)

        coVerify(exactly = 1) { service.validatePass(any(), "tok-ch") }
    }
}
