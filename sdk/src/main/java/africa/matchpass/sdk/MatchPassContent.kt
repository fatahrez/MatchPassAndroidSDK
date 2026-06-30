package africa.matchpass.sdk

/**
 * Describes the piece of content the user is trying to access.
 * Use your own IDs — MatchPass never needs to know your content database structure.
 *
 * @param id            Your content ID — must match the `external_id` registered in the
 *                      MatchPass dashboard. A mismatch returns HTTP 400.
 * @param title         Human-readable title shown on the paywall.
 * @param price         Amount as a decimal string, e.g. "29.00".
 * @param currency      ISO 4217 currency code, e.g. "ZAR".
 * @param durationHours How long the pass grants access (shown on the confirm screen).
 * @param thumbnailUrl  Optional background image shown behind the paywall panels.
 * @param contentType   Semantic type — the SDK derives the [policy] from this automatically.
 * @param policy        Override to customise cache TTL, rewatch, or UI copy for this content.
 *                      Defaults to [PassPolicy.forType] of [contentType].
 */
data class MatchPassContent(
    val id: String,
    val title: String,
    val price: String,
    val currency: String = "ZAR",
    val durationHours: Int = 4,
    val thumbnailUrl: String? = null,
    val contentType: ContentType = ContentType.MATCH,
    val policy: PassPolicy = PassPolicy.forType(contentType),
)
