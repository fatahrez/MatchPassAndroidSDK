package africa.matchpass.sdk

/**
 * Which MatchPass backend the SDK talks to. Selects the base URL — you never
 * type a URL yourself.
 */
enum class MatchPassEnvironment {
    /** Local Docker-backed MatchPassAPI. Use only for emulator development. */
    LOCAL,

    /** MatchPass's staging backend. Use this while integrating/testing. */
    STAGING,

    /** MatchPass's production backend. Default — use this for release builds. */
    PRODUCTION,
}

/**
 * Configuration for the MatchPass SDK.
 *
 * Use [MatchPassSDK.Builder] rather than constructing this directly.
 *
 * @param apiKey      Your operator API key from the MatchPass dashboard.
 * @param environment Which backend to talk to — [MatchPassEnvironment.STAGING] while
 *                     integrating, [MatchPassEnvironment.PRODUCTION] (the default) for release.
 * @param debug       Enables OkHttp request/response logging in debug builds.
 * @param colors      Explicit color override for the login/paywall screens. Leave null (the
 *                     default) to have the SDK derive its colors from your app's own
 *                     MaterialTheme automatically — see [MatchPassColors].
 */
data class MatchPassConfig(
    val apiKey: String,
    val environment: MatchPassEnvironment = MatchPassEnvironment.PRODUCTION,
    val debug: Boolean = false,
    val colors: MatchPassColors? = null,
    val errorReporter: (Throwable) -> Unit = {},
) {
    internal fun reportError(error: Throwable) {
        runCatching { errorReporter(error) }
    }

    internal val baseUrl: String
        get() = when (environment) {
            MatchPassEnvironment.LOCAL -> "http://10.0.2.2:8001/api/v1/"
            MatchPassEnvironment.STAGING -> "https://staging.api.b2b.matchpass.africa/api/v1/"
            MatchPassEnvironment.PRODUCTION -> "https://api.matchpass.africa/api/v1/"
        }
}
