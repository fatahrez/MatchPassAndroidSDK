package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.internal.ui.PaywallState
import africa.matchpass.sdk.internal.ui.SdkColors

@Composable
internal fun PaymentPanel(content: MatchPassContent, state: PaywallState) {
    OverlayCard(horizontalAlignment = Alignment.CenterHorizontally) {
        MatchPassBadge()
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = SdkColors.gold, modifier = Modifier.size(44.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(20.dp))
        Text(text = "Processing payment...", color = SdkColors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Charging ${content.priceLabel()} from MTN Mobile Money",
            color = SdkColors.textSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = state.phoneNumber, color = SdkColors.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SdkColors.card)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = content.title, color = SdkColors.textSecondary, fontSize = 12.sp)
            Text(text = content.priceLabel(), color = SdkColors.gold, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}
