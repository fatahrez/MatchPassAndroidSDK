package africa.matchpass.sdk

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The color palette the SDK's login and paywall screens render with.
 *
 * You don't need to touch this at all in the common case: if you don't set
 * one explicitly via [MatchPassSDK.Builder.colors], the SDK derives its
 * colors from whatever [MaterialTheme] is active where you call
 * [MatchPassSDK.Login] / [MatchPassSDK.Paywall] — so it automatically
 * matches your app's own theme, including if your app switches between
 * multiple themes or light/dark at runtime.
 *
 * Set one explicitly if you want the paywall to keep its own identity
 * regardless of your app's theme (e.g. a fixed "MatchPass" look inside a
 * host app that doesn't use Compose theming, or that you don't want the
 * paywall to inherit).
 */
data class MatchPassColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val primary: Color,
    val accent: Color,
    val success: Color,
    val text: Color,
    val textSecondary: Color,
    val overlay: Color,
    val error: Color,
) {
    companion object {
        /** The SDK's original fixed look — used when there's no MaterialTheme to derive from. */
        val Default = MatchPassColors(
            background = Color(0xFF0D0F1A),
            surface = Color(0xFF1A1A2E),
            card = Color(0xFF242438),
            primary = Color(0xFF0072C6),
            accent = Color(0xFFF5A623),
            success = Color(0xFF2ECC71),
            text = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF9B9BBE),
            overlay = Color(0xCC000000),
            error = Color(0xFFE53935),
        )

        /**
         * Maps a Material 3 [ColorScheme] onto [MatchPassColors]. [success] and [overlay]
         * have no direct M3 equivalent (M3 has no "success" role, and a sheet backdrop
         * dim should stay dark regardless of light/dark theme for the sheet above it to
         * read clearly) — those two stay fixed from [Default] rather than guess at a
         * mapping that could go wrong for an arbitrary host theme.
         */
        fun from(colorScheme: ColorScheme) = MatchPassColors(
            background = colorScheme.background,
            surface = colorScheme.surface,
            card = colorScheme.surfaceVariant,
            primary = colorScheme.primary,
            accent = colorScheme.tertiary,
            success = Default.success,
            text = colorScheme.onBackground,
            textSecondary = colorScheme.onSurfaceVariant,
            overlay = Default.overlay,
            error = colorScheme.error,
        )
    }
}

/**
 * Resolves the colors the SDK should render with: whatever the operator set via
 * [MatchPassSDK.Builder.colors], or the ambient [MaterialTheme] if they didn't.
 */
@Composable
internal fun resolveMatchPassColors(): MatchPassColors =
    MatchPassSDK.config.colors ?: MatchPassColors.from(MaterialTheme.colorScheme)
