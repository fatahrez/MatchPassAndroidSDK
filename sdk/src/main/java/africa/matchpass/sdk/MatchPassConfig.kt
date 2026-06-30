package africa.matchpass.sdk

/**
 * Configuration for the MatchPass SDK.
 *
 * @param apiKey  Your operator API key from the MatchPass dashboard.
 * @param baseUrl Override to point at a staging or self-hosted instance.
 * @param debug   Enables OkHttp request/response logging.
 */
data class MatchPassConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.matchpass.africa/api/v1/",
    val debug: Boolean = false,
)
