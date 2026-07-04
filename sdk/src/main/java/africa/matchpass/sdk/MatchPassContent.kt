package africa.matchpass.sdk

/**
 * Describes the piece of content the user is trying to access.
 * Use your own IDs — MatchPass never needs to know your content database structure.
 *
 * @param id            Your content ID — must match the `external_id` registered in the
 *                      MatchPass dashboard. A mismatch returns HTTP 400.
 * @param title         Human-readable title shown on the paywall.
 * @param price         Amount as a decimal string, e.g. "50.00". Must comply with the
 *                      platform pricing policy for the given [category].
 * @param currency      Currency code shown on the paywall, e.g. "KSh", "KES", "ZAR".
 * @param durationHours How long the pass grants access (shown on the confirm screen).
 * @param thumbnailUrl  Optional background image shown behind the paywall panels.
 * @param contentType   Semantic type — the SDK derives the [policy] from this automatically.
 * @param category      Broad content category used for analytics and pricing policy lookup.
 *                      See platform pricing rules: SPORTS = fixed KES 50,
 *                      ENTERTAINMENT = KES 200–300, EDUCATION/BROADCASTING = open.
 * @param policy        Override to customise cache TTL, rewatch, or UI copy for this content.
 *                      Defaults to [PassPolicy.forType] of [contentType].
 */
data class MatchPassContent(
    val id: String,
    val title: String,
    val price: String,
    val currency: String = "KES",
    val durationHours: Int = 4,
    val thumbnailUrl: String? = null,
    val contentType: ContentType = ContentType.MATCH,
    val category: ContentCategory? = null,
    val policy: PassPolicy = PassPolicy.forType(contentType),
)
