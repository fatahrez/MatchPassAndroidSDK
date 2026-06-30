package africa.matchpass.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PassPolicyTest {

    @Test
    fun `forType MATCH returns MATCH preset`() {
        assertSame(PassPolicy.MATCH, PassPolicy.forType(ContentType.MATCH))
    }

    @Test
    fun `forType CHANNEL returns CHANNEL preset`() {
        assertSame(PassPolicy.CHANNEL, PassPolicy.forType(ContentType.CHANNEL))
    }

    @Test
    fun `forType SEASON returns SEASON preset`() {
        assertSame(PassPolicy.SEASON, PassPolicy.forType(ContentType.SEASON))
    }

    @Test
    fun `forType MOVIE returns MOVIE preset`() {
        assertSame(PassPolicy.MOVIE, PassPolicy.forType(ContentType.MOVIE))
    }

    @Test
    fun `cache TTLs are ordered correctly — live content is validated more often than owned`() {
        assertTrue("CHANNEL < MATCH", PassPolicy.CHANNEL.cacheTtlSeconds < PassPolicy.MATCH.cacheTtlSeconds)
        assertTrue("MATCH < SEASON", PassPolicy.MATCH.cacheTtlSeconds < PassPolicy.SEASON.cacheTtlSeconds)
        assertTrue("SEASON < MOVIE", PassPolicy.SEASON.cacheTtlSeconds < PassPolicy.MOVIE.cacheTtlSeconds)
    }

    @Test
    fun `MATCH and CHANNEL do not allow rewatch`() {
        assertFalse(PassPolicy.MATCH.allowRewatch)
        assertFalse(PassPolicy.CHANNEL.allowRewatch)
    }

    @Test
    fun `SEASON and MOVIE allow rewatch`() {
        assertTrue(PassPolicy.SEASON.allowRewatch)
        assertTrue(PassPolicy.MOVIE.allowRewatch)
    }

    @Test
    fun `MATCH and CHANNEL show countdown`() {
        assertTrue(PassPolicy.MATCH.showCountdown)
        assertTrue(PassPolicy.CHANNEL.showCountdown)
    }

    @Test
    fun `SEASON and MOVIE do not show countdown`() {
        assertFalse(PassPolicy.SEASON.showCountdown)
        assertFalse(PassPolicy.MOVIE.showCountdown)
    }

    @Test
    fun `MatchPassContent derives policy automatically from contentType`() {
        val matchContent = MatchPassContent(id = "x", title = "T", price = "0", contentType = ContentType.MATCH)
        assertEquals(PassPolicy.MATCH, matchContent.policy)

        val movieContent = MatchPassContent(id = "x", title = "T", price = "0", contentType = ContentType.MOVIE)
        assertEquals(PassPolicy.MOVIE, movieContent.policy)
    }

    @Test
    fun `MatchPassContent allows policy override independent of contentType`() {
        val customPolicy = PassPolicy.MATCH.copy(cacheTtlSeconds = 999L)
        val content = MatchPassContent(
            id = "x", title = "T", price = "0",
            contentType = ContentType.MATCH,
            policy = customPolicy,
        )
        assertEquals(999L, content.policy.cacheTtlSeconds)
    }

    @Test
    fun `default contentType is MATCH`() {
        val content = MatchPassContent(id = "x", title = "T", price = "0")
        assertEquals(ContentType.MATCH, content.contentType)
        assertEquals(PassPolicy.MATCH, content.policy)
    }
}
