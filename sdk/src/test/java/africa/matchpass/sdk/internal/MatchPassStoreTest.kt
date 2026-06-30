package africa.matchpass.sdk.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MatchPassStoreTest {

    private lateinit var store: MatchPassStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = MatchPassStore(context)
    }

    // ── Pass token ─────────────────────────────────────────────────────────────

    @Test
    fun `getToken returns null when nothing saved`() {
        assertNull(store.getToken("unknown-content"))
    }

    @Test
    fun `savePass and getToken round-trip`() {
        store.savePass("epl-match-001", "tok-abc")
        assertEquals("tok-abc", store.getToken("epl-match-001"))
    }

    @Test
    fun `clearPass removes token`() {
        store.savePass("epl-match-001", "tok-abc")
        store.clearPass("epl-match-001")
        assertNull(store.getToken("epl-match-001"))
    }

    @Test
    fun `clearPass removes validation timestamp`() {
        store.savePass("epl-match-001", "tok-abc")
        store.saveValidationTime("epl-match-001", 12345L)
        store.clearPass("epl-match-001")
        assertEquals(0L, store.getValidationTime("epl-match-001"))
    }

    @Test
    fun `tokens are isolated per contentId`() {
        store.savePass("content-a", "tok-a")
        store.savePass("content-b", "tok-b")
        assertEquals("tok-a", store.getToken("content-a"))
        assertEquals("tok-b", store.getToken("content-b"))
        store.clearPass("content-a")
        assertNull(store.getToken("content-a"))
        assertEquals("tok-b", store.getToken("content-b"))
    }

    // ── Validation timestamp ───────────────────────────────────────────────────

    @Test
    fun `getValidationTime returns 0 when nothing saved`() {
        assertEquals(0L, store.getValidationTime("unknown-content"))
    }

    @Test
    fun `saveValidationTime and getValidationTime round-trip`() {
        store.saveValidationTime("epl-match-001", 1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, store.getValidationTime("epl-match-001"))
    }

    @Test
    fun `clearPass also removes expiry timestamp`() {
        store.savePass("epl-match-001", "tok-abc")
        store.saveExpiresAt("epl-match-001", 9_999_999_999_000L)
        store.clearPass("epl-match-001")
        assertEquals(0L, store.getExpiresAt("epl-match-001"))
    }

    // ── Expiry timestamp ───────────────────────────────────────────────────────

    @Test
    fun `getExpiresAt returns 0 when nothing saved`() {
        assertEquals(0L, store.getExpiresAt("unknown-content"))
    }

    @Test
    fun `saveExpiresAt and getExpiresAt round-trip`() {
        store.saveExpiresAt("epl-match-001", 9_999_999_999_000L)
        assertEquals(9_999_999_999_000L, store.getExpiresAt("epl-match-001"))
    }

    // ── Phone ──────────────────────────────────────────────────────────────────

    @Test
    fun `getPhone returns empty string when nothing saved`() {
        assertEquals("", store.getPhone())
    }

    @Test
    fun `savePhone and getPhone round-trip`() {
        store.savePhone("+27821234567")
        assertEquals("+27821234567", store.getPhone())
    }
}
