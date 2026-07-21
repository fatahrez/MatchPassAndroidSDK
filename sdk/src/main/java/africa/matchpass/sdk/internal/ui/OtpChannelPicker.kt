package africa.matchpass.sdk.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Only ever rendered when there's more than one enabled channel to choose
 * between (callers gate on `availableChannels.size > 1`) — a single-channel
 * operator sees no picker at all, keeping the common case's UI unchanged.
 */
@Composable
internal fun OtpChannelPicker(
    channels: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    val colors = LocalMatchPassColors.current
    Column {
        Text(
            text = "Send code via",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { channel ->
                val isSelected = channel == selected
                Text(
                    text = otpChannelLabel(channel),
                    color = if (isSelected) colors.background else colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) colors.accent else colors.card)
                        .border(1.dp, if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                        .clickable { onSelect(channel) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}
