package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.internal.ui.SdkColors

@Composable
internal fun AccessGrantedPanel(content: MatchPassContent, onWatch: () -> Unit) {
    OverlayCard(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SdkColors.green,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(text = "Access Granted!", color = SdkColors.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(text = content.title, color = SdkColors.textSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your MatchPass is active for ${content.durationHours}h",
            color = SdkColors.gold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onWatch,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SdkColors.blue),
        ) {
            Text("Watch Now", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}
