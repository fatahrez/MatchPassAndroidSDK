package africa.matchpass.sdk.internal.ui.panels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.internal.ui.LocalMatchPassColors

@Composable
internal fun IssuingPanel(label: String) {
    val colors = LocalMatchPassColors.current
    OverlayCard(horizontalAlignment = Alignment.CenterHorizontally) {
        MatchPassBadge()
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(text = label, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
