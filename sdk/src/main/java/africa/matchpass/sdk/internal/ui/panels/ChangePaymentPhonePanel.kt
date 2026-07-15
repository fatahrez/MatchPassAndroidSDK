package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.internal.ui.PaywallState
import africa.matchpass.sdk.internal.ui.LocalMatchPassColors

@Composable
internal fun ChangePaymentPhonePanel(
    state: PaywallState,
    onPhoneChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalMatchPassColors.current
    OverlayCard {
        MatchPassBadge()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Pay with a different number",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Enter the M-Pesa number to receive the payment prompt. Your account stays linked to ${state.phoneNumber}.",
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            value = state.editingPaymentPhone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. 0712 345 678", color = colors.textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedIndicatorColor = colors.success,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = colors.success,
            ),
            shape = RoundedCornerShape(8.dp),
        )
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = colors.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.success),
            enabled = state.editingPaymentPhone.isNotBlank(),
        ) {
            Text("Use this number", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}
