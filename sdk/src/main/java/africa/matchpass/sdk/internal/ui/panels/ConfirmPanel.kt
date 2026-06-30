package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
internal fun ConfirmPanel(
    content: MatchPassContent,
    state: PaywallState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayCard {
        MatchPassBadge()
        Spacer(Modifier.height(16.dp))
        Text(text = "Confirm your purchase", color = SdkColors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SdkColors.card)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = content.title, color = SdkColors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "${content.durationHours}h access", color = SdkColors.textSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(text = content.priceLabel(), color = SdkColors.gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Charged to ${state.phoneNumber}",
            color = SdkColors.textSecondary,
            fontSize = 11.sp,
        )
        state.error?.let { Text(text = it, color = SdkColors.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SdkColors.green),
        ) {
            Text("Confirm & Watch — ${content.priceLabel()}", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = SdkColors.textSecondary, fontSize = 13.sp)
        }
    }
}
