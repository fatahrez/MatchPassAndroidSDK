package africa.matchpass.sdk

import android.content.Context
import androidx.compose.runtime.Composable
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.MatchPassStore
import africa.matchpass.sdk.internal.ui.PaywallScreen

/**
 * MatchPass SDK entry point.
 *
 * ## Setup (once, in Application.onCreate or before first use)
 * ```kotlin
 * MatchPassSDK.init(
 *     context = applicationContext,
 *     config  = MatchPassConfig(apiKey = "your-api-key"),
 * )
 * ```
 *
 * ## Show the paywall
 * ```kotlin
 * MatchPassSDK.Paywall(
 *     content         = MatchPassContent(id = "match-123", title = "Arsenal vs Man City", price = "29.00"),
 *     onAccessGranted = { grant -> startPlayer(grant.token) },
 *     onDismiss       = { navController.popBackStack() },
 * )
 * ```
 *
 * ## Check access without UI
 * ```kotlin
 * val hasAccess = MatchPassSDK.hasAccess(context, contentId = "match-123")
 * ```
 */
object MatchPassSDK {

    internal lateinit var config: MatchPassConfig
        private set

    internal lateinit var client: MatchPassClient
        private set

    private var initialised = false

    fun init(context: Context, config: MatchPassConfig) {
        if (initialised) return
        this.config = config
        this.client = MatchPassClient(config)
        initialised = true
    }

    /**
     * Drop this composable wherever you'd normally show a subscriber paywall.
     * It manages its own state — no ViewModel wiring needed from your side.
     *
     * If the user already has a valid pass for [content], it validates silently
     * and calls [onAccessGranted] immediately without showing any UI.
     */
    @Composable
    fun Paywall(
        content: MatchPassContent,
        onAccessGranted: (MatchPassGrant) -> Unit,
        onDismiss: () -> Unit,
    ) {
        checkInitialised()
        PaywallScreen(
            content = content,
            client = client,
            onAccessGranted = onAccessGranted,
            onDismiss = onDismiss,
        )
    }

    /**
     * Returns true if there is a locally-stored, server-validated pass for [contentId].
     * Safe to call on any coroutine — does not touch the main thread.
     */
    suspend fun hasAccess(context: Context, contentId: String): Boolean {
        checkInitialised()
        val token = MatchPassStore(context).getToken(contentId) ?: return false
        return runCatching {
            client.service.validatePass("ApiKey ${config.apiKey}", token).isValid
        }.getOrDefault(false)
    }

    private fun checkInitialised() {
        check(initialised) {
            "MatchPassSDK.init() must be called before use. Call it in Application.onCreate()."
        }
    }
}
