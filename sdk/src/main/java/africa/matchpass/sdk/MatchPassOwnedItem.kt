package africa.matchpass.sdk

/**
 * A single item the user has an active pass for. Returned by [MatchPassSDK.getMyPasses].
 */
data class MatchPassOwnedItem(
    val contentId: String,
    val title: String,
    val contentType: String,
    val amount: String,
    val currency: String,
    val issuedAt: String,
    /** ISO 8601 expiry timestamp, or null for a lifetime pass (movies/series) that never expires. */
    val expiresAt: String?,
)
