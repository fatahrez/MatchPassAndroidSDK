package africa.matchpass.sdk

/**
 * All errors thrown by the MatchPass SDK are subtypes of [MatchPassException].
 * Catch the sealed subtype you care about; fall through to the base class for
 * anything unexpected.
 *
 * ```kotlin
 * try {
 *     val result = MatchPassSDK.checkAccess(context, contentId)
 * } catch (e: MatchPassException.NetworkError) {
 *     showOfflineMessage()
 * } catch (e: MatchPassException) {
 *     showGenericError(e.message)
 * }
 * ```
 */
sealed class MatchPassException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** [MatchPassSDK.init] was not called before using the SDK. */
    class NotInitialized :
        MatchPassException("MatchPassSDK.init() must be called before use. Call it in Application.onCreate().")

    /** The supplied [contentId] does not match any active content for this operator. */
    class ContentNotFound(contentId: String) :
        MatchPassException("Content not found or not active: '$contentId'. Verify the id matches the external_id registered in the MatchPass dashboard.")

    /** The OTP code was incorrect. */
    class InvalidOtp :
        MatchPassException("The OTP code is incorrect. Please check and try again.")

    /** The OTP code has expired. A new one must be requested. */
    class OtpExpired :
        MatchPassException("The OTP has expired. Please request a new code.")

    /** The pass exists but is past its expiry date. */
    class PassExpired(val expiredAt: String) :
        MatchPassException("Pass expired at $expiredAt.")

    /** The pass was explicitly revoked by the operator. */
    class PassRevoked :
        MatchPassException("This pass has been revoked.")

    /** A network call failed. Check [cause] for the underlying IOException. */
    class NetworkError(cause: Throwable) :
        MatchPassException(africa.matchpass.sdk.internal.networkFailureMessage(), cause)

    /** The server returned an unexpected HTTP error. [rawBody] is kept for logging only —
     * never shown to the user, [message] is already a plain-language equivalent. */
    class ServerError(val code: Int, val rawBody: String) :
        MatchPassException(africa.matchpass.sdk.internal.httpStatusToFriendlyMessage(code))

    /** The SDK was not configured correctly. See [message] for details. */
    class ConfigurationError(message: String) :
        MatchPassException(message)
}
