package africa.matchpass.sdk

/**
 * Describes the piece of content the user is trying to access.
 * Use your own IDs — MatchPass never needs to know your content database structure.
 *
 * @param id            Your content ID (external_id in MatchPass).
 * @param title         Human-readable title shown on the paywall.
 * @param price         Amount as a string, e.g. "29.00".
 * @param currency      ISO 4217 currency code, e.g. "ZAR".
 * @param durationHours How long the pass grants access for.
 * @param thumbnailUrl  Optional background image shown behind the paywall.
 */
data class MatchPassContent(
    val id: String,
    val title: String,
    val price: String,
    val currency: String = "ZAR",
    val durationHours: Int = 4,
    val thumbnailUrl: String? = null,
)
