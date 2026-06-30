package africa.matchpass.sdk

/**
 * Returned when a pass is successfully issued or an existing pass is validated.
 *
 * Present [token] to your streaming backend to authorise playback.
 */
data class MatchPassGrant(
    val token: String,
    val contentId: String,
    val expiresAt: String,
)
