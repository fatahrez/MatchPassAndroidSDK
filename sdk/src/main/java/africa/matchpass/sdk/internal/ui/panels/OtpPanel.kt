package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.internal.ui.PaywallState
import africa.matchpass.sdk.internal.ui.LocalMatchPassColors

@Composable
internal fun OtpPanel(
    state: PaywallState,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMatchPassColors.current
    OverlayCard {
        MatchPassBadge()
        Spacer(Modifier.height(16.dp))
        Text(text = "Enter the OTP sent to", color = colors.textSecondary, fontSize = 13.sp)
        Text(text = state.phoneNumber, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        state.demoOtp?.let { otp ->
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.card)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Demo OTP", color = colors.textSecondary, fontSize = 11.sp)
                Text(text = otp, color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.otpCode,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            placeholder = { Text("6-digit code", color = colors.textSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.card,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                cursorColor = colors.primary,
            ),
        )
        state.error?.let { Text(text = it, color = colors.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onVerify,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            enabled = state.otpCode.length == 6,
        ) {
            Text("Verify OTP", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}
