package africa.matchpass.sdk

import android.content.Context
import androidx.compose.runtime.Composable
import africa.matchpass.sdk.internal.AccessChecker
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.MatchPassStore
import africa.matchpass.sdk.internal.ui.LoginScreen
import africa.matchpass.sdk.internal.ui.PaywallScreen

/**
 * MatchPass SDK entry point.
 *
 * ## Initialise (once, in Application.onCreate)
 *
 * ```kotlin
 * // Option A — Builder (recommended)
 * MatchPassSDK.Builder(this)
 *     .apiKey("your-operator-api-key")
 *     .baseUrl("https://api.matchpass.africa/api/v1/")
 *     .debug(BuildConfig.DEBUG)
 *     .initialize()
 *
 * // Option B — direct init
 * MatchPassSDK.init(
 *     context = this,
 *     config  = MatchPassConfig(apiKey = "your-operator-api-key"),
 * )
 * ```
 *
 * ## Show the paywall
 *
 * ```kotlin
 * MatchPassSDK.Paywall(
 *     content         = MatchPassContent(id = "epl-match-001", title = "Arsenal vs Man City",
 *                           price = "29.00", contentType = ContentType.MATCH),
 *     onAccessGranted = { grant -> startStream(grant.token) },
 *     onDismiss       = { navController.popBackStack() },
 * )
 * ```
 *
 * ## Check access without UI
 *
 * ```kotlin
 * when (val result = MatchPassSDK.checkAccess(context, content)) {
 *     is AccessResult.Granted      -> showResumeButton(result.grant)
 *     is AccessResult.Expired      -> showRenewalPrompt()
 *     is AccessResult.NotPurchased -> showWatchButton()
 *     is AccessResult.Error        -> showWatchButton()   // fail open on network errors
 * }
 * ```
 */
object MatchPassSDK {

    internal lateinit var config: MatchPassConfig
        private set

    internal lateinit var client: MatchPassClient
        private set

    private var initialised = false

    // ── Initialisation ──────────────────────────────────────────────────────

    /**
     * Fluent builder — preferred over calling [init] directly.
     *
     * ```kotlin
     * MatchPassSDK.Builder(this)
     *     .apiKey("your-operator-api-key")
     *     .debug(BuildConfig.DEBUG)
     *     .initialize()
     * ```
     */
    class Builder(private val context: Context) {
        private var apiKey: String? = null
        private var baseUrl: String = "https://api.matchpass.africa/api/v1/"
        private var debug: Boolean = false

        fun apiKey(key: String) = apply { apiKey = key }
        fun baseUrl(url: String) = apply { baseUrl = url }
        fun debug(enabled: Boolean) = apply { debug = enabled }

        fun initialize() {
            val key = checkNotNull(apiKey) {
                "apiKey is required. Call Builder.apiKey(\"...\") before initialize()."
            }
            init(context, MatchPassConfig(apiKey = key, baseUrl = baseUrl, debug = debug))
        }
    }

    /**
     * Initialise the SDK. Safe to call more than once — subsequent calls are no-ops.
     * Prefer [Builder] for discoverable configuration.
     */
    @JvmStatic
    fun init(context: Context, config: MatchPassConfig) {
        if (initialised) return
        this.config = config
        this.client = MatchPassClient(config)
        initialised = true
    }

    // ── Paywall composable ───────────────────────────────────────────────────

    /**
     * Drop-in paywall composable. Manages its own state — no ViewModel wiring needed.
     *
     * **Flow:**
     * - Existing valid pass → validates silently, calls [onAccessGranted] immediately.
     * - Known phone (returned user or [userPhone] supplied) → skips OTP, shows purchase confirmation.
     * - First-time user → phone entry → OTP (one-time login) → confirmation → payment.
     *
     * @param userPhone  If the host app already knows the user's phone (they're signed in),
     *                   pass it here to skip the OTP step entirely.
     */
    @Composable
    fun Paywall(
        content: MatchPassContent,
        onAccessGranted: (MatchPassGrant) -> Unit,
        onDismiss: () -> Unit,
        userPhone: String? = null,
    ) {
        checkInitialised()
        PaywallScreen(
            content = content,
            client = client,
            onAccessGranted = onAccessGranted,
            onDismiss = onDismiss,
            userPhone = userPhone,
        )
    }

    // ── Login composable ─────────────────────────────────────────────────────

    /**
     * Drop-in login composable. Full-screen OTP phone verification — skippable.
     *
     * **When to use:**
     * - On first app launch (user hasn't identified themselves yet).
     * - From a "Sign In" button in your app's top bar.
     * - If your app has no existing auth and you want MatchPass to handle identity.
     *
     * **When NOT to use:**
     * - If the host app already has an authenticated user (DStv account, Canal+ ID).
     *   Instead pass `userPhone = account.msisdn` to [Paywall] — OTP is skipped entirely.
     *
     * Once verified, the phone is persisted to local storage.
     * All subsequent [Paywall] calls will skip OTP automatically.
     *
     * @param onLoggedIn Called with the verified phone number after successful OTP.
     * @param onSkip     Called when the user dismisses without signing in.
     */
    @Composable
    fun Login(
        onLoggedIn: (phone: String) -> Unit,
        onSkip: () -> Unit,
    ) {
        checkInitialised()
        LoginScreen(
            client = client,
            onLoggedIn = onLoggedIn,
            onSkip = onSkip,
        )
    }

    /**
     * Returns the phone number verified in a previous session, or `null` if the user
     * has never completed OTP login on this device.
     *
     * Use this on app start to decide whether to show the login gate or go straight
     * to the home screen.
     */
    fun getStoredPhone(context: Context): String? {
        checkInitialised()
        return MatchPassStore(context).getPhone().ifBlank { null }
    }

    /**
     * Returns the epoch-millisecond timestamp at which the pass for [contentId] expires,
     * or `null` if no pass is stored or the expiry was never recorded.
     *
     * Use this to schedule UI re-checks precisely when a pass expires rather than polling
     * on a fixed interval.
     */
    fun getExpiresAt(context: Context, contentId: String): Long? {
        checkInitialised()
        val ms = MatchPassStore(context).getExpiresAt(contentId)
        return if (ms > 0L) ms else null
    }

    // ── Programmatic access checks ───────────────────────────────────────────

    /**
     * Checks whether the user has a valid pass for [content], returning a typed [AccessResult].
     *
     * Respects [PassPolicy.cacheTtlSeconds] — for owned content (SEASON, MOVIE) this will
     * return [AccessResult.Granted] from cache without hitting the server.
     *
     * Safe to call on any coroutine dispatcher.
     *
     * ```kotlin
     * when (val result = MatchPassSDK.checkAccess(context, content)) {
     *     is AccessResult.Granted      -> resumeStream(result.grant.token)
     *     is AccessResult.Expired      -> showRenewal()
     *     is AccessResult.NotPurchased -> showPaywall()
     *     is AccessResult.Error        -> showWatchButton()  // fail open
     * }
     * ```
     */
    suspend fun checkAccess(context: Context, content: MatchPassContent): AccessResult {
        checkInitialised()
        val store = MatchPassStore(context)
        val checker = AccessChecker(config, client.service, store)
        return checker.check(content)
    }

    /**
     * Convenience overload — returns `true` if the user has any valid pass for [contentId].
     *
     * Uses the default [PassPolicy.MATCH] cache TTL. For content-type-aware caching,
     * use [checkAccess] with a full [MatchPassContent] instead.
     */
    suspend fun hasAccess(context: Context, contentId: String): Boolean =
        checkAccess(
            context,
            MatchPassContent(id = contentId, title = "", price = "0"),
        ) is AccessResult.Granted

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun checkInitialised() {
        if (!initialised) throw MatchPassException.NotInitialized()
    }
}
