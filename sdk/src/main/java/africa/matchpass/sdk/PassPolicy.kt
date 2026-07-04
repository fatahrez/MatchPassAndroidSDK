package africa.matchpass.sdk

/**
 * Encodes how the SDK should behave for a given [ContentType].
 *
 * You rarely need to instantiate this directly — use the [ContentType]-aware
 * presets via [PassPolicy.forType], or pass a [ContentType] to [MatchPassContent]
 * and let the SDK pick the right policy automatically.
 *
 * @param cacheTtlSeconds   How long the SDK trusts a locally-cached validation before
 *                          hitting the server again. Set conservatively for live content,
 *                          liberally for owned/perpetual content.
 * @param allowRewatch      Whether the user can return to the content freely after first
 *                          access (SEASON, MOVIE). When false, each session checks the
 *                          pass expiry strictly (MATCH, CHANNEL).
 * @param showCountdown     Whether to display a "X hours remaining" indicator on the
 *                          access-granted screen. Meaningful for time-limited passes.
 * @param passLabel         Human-readable label shown on the paywall confirm screen,
 *                          e.g. "Game Pass", "Channel Pass", "Season Pass", "Own it".
 */
data class PassPolicy(
    val cacheTtlSeconds: Long,
    val allowRewatch: Boolean,
    val showCountdown: Boolean,
    val passLabel: String,
) {
    companion object {

        /** Single match or event — live, short window, frequent re-validation. */
        @JvmField
        val MATCH = PassPolicy(
            cacheTtlSeconds = 5 * 60L,
            allowRewatch = false,
            showCountdown = true,
            passLabel = "Game Pass",
        )

        /** Live channel access — streaming, re-validate every minute. */
        @JvmField
        val CHANNEL = PassPolicy(
            cacheTtlSeconds = 60L,
            allowRewatch = false,
            showCountdown = true,
            passLabel = "Channel Pass",
        )

        /** Full season ownership — rewatch freely, validate once per day. */
        @JvmField
        val SEASON = PassPolicy(
            cacheTtlSeconds = 24 * 60 * 60L,
            allowRewatch = true,
            showCountdown = false,
            passLabel = "Season Pass",
        )

        /** Perpetual movie ownership — rewatch anytime, validate once per month. */
        @JvmField
        val MOVIE = PassPolicy(
            cacheTtlSeconds = 30 * 24 * 60 * 60L,
            allowRewatch = true,
            showCountdown = false,
            passLabel = "Own it",
        )

        /** Special event (comedy night, concert, graduation) — same as MATCH: live window, frequent checks. */
        @JvmField
        val EVENT = PassPolicy(
            cacheTtlSeconds = 5 * 60L,
            allowRewatch = false,
            showCountdown = true,
            passLabel = "Event Pass",
        )

        /** Returns the canonical [PassPolicy] for a given [ContentType]. */
        @JvmStatic
        fun forType(type: ContentType): PassPolicy = when (type) {
            ContentType.MATCH   -> MATCH
            ContentType.CHANNEL -> CHANNEL
            ContentType.SEASON  -> SEASON
            ContentType.MOVIE   -> MOVIE
            ContentType.EVENT   -> EVENT
        }
    }
}
