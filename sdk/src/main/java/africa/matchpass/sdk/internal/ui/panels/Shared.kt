package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.internal.ui.LocalMatchPassColors

@Composable
internal fun OverlayCard(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMatchPassColors.current
    // Every panel (paywall, OTP, payment...) renders through here, so this
    // is the one place to fix width for all of them at once. Same tablet
    // issue as a plain fillMaxWidth() elsewhere: capped and centered so it
    // reads as a proper bottom sheet/dialog instead of stretching to the
    // full device width. widthIn(max) has to come before fillMaxWidth() —
    // reversed, fillMaxWidth()'s exact-width constraint would make the cap
    // a no-op (see DetailScreen.kt in the app repo for the full story).
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.surface)
                .padding(24.dp),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

@Composable
internal fun MatchPassBadge() {
    val colors = LocalMatchPassColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "Powered by MatchPass",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun MatchPassContent.priceLabel() = "$currency $price"
