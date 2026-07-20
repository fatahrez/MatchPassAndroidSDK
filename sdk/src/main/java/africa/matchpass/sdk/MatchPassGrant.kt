package africa.matchpass.sdk

/**
 * Returned when a pass is successfully issued or an existing pass is validated.
 *
 * Present [token] to your streaming backend to authorise playback.
 */
data class MatchPassGrant(
    val token: String,
    val contentId: String,
    /** ISO 8601 expiry timestamp, or null for a lifetime pass (movies/series) that never expires. */
    val expiresAt: String?,
)
