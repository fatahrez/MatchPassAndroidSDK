package africa.matchpass.sdk

/**
 * Result of [MatchPassSDK.checkAccess]. Always inspect the type before acting —
 * do not assume access is granted.
 *
 * ```kotlin
 * when (val result = MatchPassSDK.checkAccess(context, content)) {
 *     is AccessResult.Granted       -> startStream(result.grant.token)
 *     is AccessResult.Expired       -> showRenewal("Expired at ${result.expiredAt}")
 *     is AccessResult.NotPurchased  -> showPaywall()
 *     is AccessResult.Error         -> showError(result.exception.message)
 * }
 * ```
 */
sealed class AccessResult {

    /** A valid pass exists. Use [grant] to start the stream. */
    data class Granted(val grant: MatchPassGrant) : AccessResult()

    /**
     * A pass was found but it has expired.
     * [expiredAt] is an ISO 8601 timestamp, or empty if the server did not return one.
     * Show a renewal prompt — the user must purchase again.
     */
    data class Expired(val expiredAt: String) : AccessResult()

    /**
     * No pass was found locally or it was cleared. Show the paywall or a purchase prompt.
     * This is the normal state for a first-time viewer.
     */
    data object NotPurchased : AccessResult()

    /**
     * The check failed due to a network or server error. The locally-cached pass
     * (if any) was NOT cleared — assume the user still has access rather than
     * blocking them on a transient error.
     */
    data class Error(val exception: MatchPassException) : AccessResult()
}
