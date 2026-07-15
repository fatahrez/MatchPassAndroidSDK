package africa.matchpass.sdk

/**
 * Configuration for the MatchPass SDK.
 *
 * Use [MatchPassSDK.Builder] rather than constructing this directly.
 *
 * @param apiKey  Your operator API key from the MatchPass dashboard.
 * @param debug   Enables OkHttp request/response logging in debug builds.
 * @param baseUrl Staging / local-dev override. Defaults to the MatchPass production URL.
 *                You do not need to set this for production use.
 * @param colors  Explicit color override for the login/paywall screens. Leave null (the
 *                default) to have the SDK derive its colors from your app's own
 *                MaterialTheme automatically — see [MatchPassColors].
 */
data class MatchPassConfig(
    val apiKey: String,
    val debug: Boolean = false,
    val baseUrl: String = "https://api.matchpass.africa/api/v1/",
    val colors: MatchPassColors? = null,
)
