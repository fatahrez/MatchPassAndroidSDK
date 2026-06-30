package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.internal.ui.PaywallState
import africa.matchpass.sdk.internal.ui.SdkColors

@Composable
internal fun PhonePanel(
    content: MatchPassContent,
    state: PaywallState,
    onPhoneChange: (String) -> Unit,
    onRequestOtp: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayCard {
        MatchPassBadge()
        Spacer(Modifier.height(16.dp))
        Text(text = content.title, color = SdkColors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${content.priceLabel()} · ${content.durationHours}h access · No subscription needed",
            color = SdkColors.textSecondary,
            fontSize = 12.sp,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = SdkColors.card)
        Text(text = "Enter your mobile number", color = SdkColors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = onPhoneChange,
            placeholder = { Text("+27 82 123 4567", color = SdkColors.textSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SdkColors.blue,
                unfocusedBorderColor = SdkColors.card,
                focusedTextColor = SdkColors.text,
                unfocusedTextColor = SdkColors.text,
                cursorColor = SdkColors.blue,
            ),
        )
        state.error?.let { Text(text = it, color = SdkColors.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequestOtp,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SdkColors.blue),
            enabled = state.phoneNumber.isNotBlank(),
        ) {
            Text("Get OTP", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Maybe Later", color = SdkColors.textSecondary, fontSize = 13.sp)
        }
    }
}
