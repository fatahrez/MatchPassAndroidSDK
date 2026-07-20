package africa.matchpass.sdk.internal

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassListFetcherTest {

    private val service = mockk<MatchPassService>()

    @Test
    fun `returns empty list when there are no passes`() = runTest {
        coEvery { service.listPasses(any(), "254700000000", "active", 1) } returns
            PassListResponseDto(count = 0, next = null, results = emptyList())

        val result = fetchAllOwnedPasses(service, "test-key", "254700000000")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `maps a single page of results to owned items`() = runTest {
        coEvery { service.listPasses(any(), "254700000000", "active", 1) } returns
            PassListResponseDto(
                count = 1,
                next = null,
                results = listOf(
                    PassSummaryDto(
                        contentId = "movie-1",
                        contentTitle = "The Woman King",
                        contentType = "movie",
                        status = "active",
                        amount = "349.00",
                        currency = "KSh",
                        issuedAt = "2026-07-01T13:00:40Z",
                        expiresAt = null,
                    ),
                ),
            )

        val result = fetchAllOwnedPasses(service, "test-key", "254700000000")

        assertEquals(1, result.size)
        assertEquals("movie-1", result[0].contentId)
        assertEquals("The Woman King", result[0].title)
        assertNull(result[0].expiresAt)
    }

    @Test
    fun `follows pagination until next is null`() = runTest {
        coEvery { service.listPasses(any(), "254700000000", "active", 1) } returns
            PassListResponseDto(
                count = 2,
                next = "https://staging.api.matchpass.africa/api/v1/passes/?page=2",
                results = listOf(PassSummaryDto(contentId = "movie-1", contentTitle = "First")),
            )
        coEvery { service.listPasses(any(), "254700000000", "active", 2) } returns
            PassListResponseDto(count = 2, next = null, results = listOf(PassSummaryDto(contentId = "movie-2", contentTitle = "Second")))

        val result = fetchAllOwnedPasses(service, "test-key", "254700000000")

        assertEquals(2, result.size)
        assertEquals(listOf("movie-1", "movie-2"), result.map { it.contentId })
    }

    @Test
    fun `passes the api key as an ApiKey authorization header`() = runTest {
        coEvery { service.listPasses("ApiKey test-key", "254700000000", "active", 1) } returns
            PassListResponseDto(count = 0, next = null, results = emptyList())

        fetchAllOwnedPasses(service, "test-key", "254700000000")

        io.mockk.coVerify { service.listPasses("ApiKey test-key", "254700000000", "active", 1) }
    }
}
