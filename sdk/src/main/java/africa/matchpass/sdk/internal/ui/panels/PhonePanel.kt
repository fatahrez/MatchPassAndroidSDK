package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.internal.ui.PaywallState
import africa.matchpass.sdk.internal.ui.LocalMatchPassColors
import africa.matchpass.sdk.internal.ui.OtpChannelPicker
import africa.matchpass.sdk.internal.ui.PhoneNumberField

@Composable
internal fun PhonePanel(
    content: MatchPassContent,
    state: PaywallState,
    onPhoneChange: (String) -> Unit,
    onRequestOtp: () -> Unit,
    onDismiss: () -> Unit,
    onSelectChannel: (String) -> Unit = {},
) {
    val colors = LocalMatchPassColors.current
    OverlayCard {
        MatchPassBadge()
        Spacer(Modifier.height(16.dp))
        Text(text = content.title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${content.priceLabel()} · ${content.durationHours}h access · No subscription needed",
            color = colors.textSecondary,
            fontSize = 12.sp,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colors.card)
        Text(text = "Verify your number", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "One-time only — future purchases will be instant",
            color = colors.textSecondary,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))
        PhoneNumberField(
            phone = state.phoneNumber,
            onPhoneChange = onPhoneChange,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(text = it, color = colors.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        if (state.availableChannels.size > 1) {
            Spacer(Modifier.height(12.dp))
            OtpChannelPicker(
                channels = state.availableChannels,
                selected = state.selectedChannel,
                onSelect = onSelectChannel,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequestOtp,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            enabled = state.phoneNumber.isNotBlank() && !state.isLoading,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Send verification code", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Maybe Later", color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}
