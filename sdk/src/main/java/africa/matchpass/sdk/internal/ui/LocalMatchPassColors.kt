package africa.matchpass.sdk.internal.ui

import androidx.compose.runtime.staticCompositionLocalOf
import africa.matchpass.sdk.MatchPassColors

/**
 * Provided once, at the top of LoginScreen/PaywallScreen, from
 * [africa.matchpass.sdk.resolveMatchPassColors]. Every internal composable
 * below reads this rather than a fixed palette, so the whole SDK actually
 * responds to an operator-set override or the host app's theme changing —
 * something a plain singleton object never could, since it can't participate
 * in recomposition.
 */
internal val LocalMatchPassColors = staticCompositionLocalOf { MatchPassColors.Default }
