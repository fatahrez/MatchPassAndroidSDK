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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.internal.ui.PaywallState
import africa.matchpass.sdk.internal.ui.SdkColors

private val MpesaGreen = Color(0xFF4CAF50)

@Composable
internal fun PaymentPanel(content: MatchPassContent, state: PaywallState) {
    OverlayCard(horizontalAlignment = Alignment.CenterHorizontally) {
        MatchPassBadge()
        Spacer(Modifier.height(20.dp))

        // M-Pesa label
        Text(
            text = "M-PESA",
            color = MpesaGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(16.dp))

        CircularProgressIndicator(color = MpesaGreen, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Check your phone",
            color = SdkColors.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Enter your M-Pesa PIN on\n${state.phoneNumber} to complete payment",
            color = SdkColors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
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
            Text(text = content.title, color = SdkColors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(text = content.priceLabel(), color = MpesaGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}
