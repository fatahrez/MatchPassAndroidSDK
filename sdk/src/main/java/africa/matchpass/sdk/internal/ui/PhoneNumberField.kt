package africa.matchpass.sdk.internal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassColors

internal data class Country(val name: String, val isoCode: String, val dialCode: String)

// Markets MatchPass and its pay-TV-style peers (DStv, Canal+) actually
// operate in. Kenya first/default — the primary documented market
// (KES pricing, M-Pesa) — the rest cover the SDK's wider pan-African reach.
internal val COUNTRIES = listOf(
    Country("Kenya", "KE", "+254"),
    Country("South Africa", "ZA", "+27"),
    Country("Nigeria", "NG", "+234"),
    Country("Ghana", "GH", "+233"),
    Country("Uganda", "UG", "+256"),
    Country("Tanzania", "TZ", "+255"),
    Country("Rwanda", "RW", "+250"),
    Country("Zambia", "ZM", "+260"),
    Country("Ethiopia", "ET", "+251"),
    Country("Egypt", "EG", "+20"),
)

/**
 * Country-code dropdown + local-number field, combined into the single phone
 * string [onPhoneChange] expects (matching the existing "+254712345678"
 * shape the rest of the SDK already works with — no state-model change).
 */
@Composable
internal fun PhoneNumberField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    colors: MatchPassColors,
    modifier: Modifier = Modifier,
) {
    var selectedCountry by rememberSaveable(stateSaver = countrySaver) {
        mutableStateOf(countryFor(phone) ?: COUNTRIES.first())
    }
    var expanded by remember { mutableStateOf(false) }
    val localNumber = phone.removePrefix(selectedCountry.dialCode)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Styled to match the OutlinedTextField beside it — same corner
        // radius, same border color/width behavior (1.dp unfocused, 2.dp
        // "focused" while the menu is open), same 56.dp min height M3 gives
        // OutlinedTextField by default — so the two read as one control.
        val borderColor by animateColorAsState(if (expanded) colors.primary else colors.card, label = "picker-border-color")
        val borderWidth = if (expanded) 2.dp else 1.dp
        val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "picker-chevron-rotation")

        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .widthIn(min = 96.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selectedCountry.dialCode, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Choose country",
                tint = colors.textSecondary,
                modifier = Modifier.rotate(chevronRotation),
            )

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                COUNTRIES.forEach { country ->
                    DropdownMenuItem(
                        text = { Text("${country.name}  ${country.dialCode}") },
                        onClick = {
                            selectedCountry = country
                            expanded = false
                            onPhoneChange(country.dialCode + localNumber)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        OutlinedTextField(
            value = localNumber,
            onValueChange = { digits -> onPhoneChange(selectedCountry.dialCode + digits.filter { it.isDigit() }) },
            placeholder = { Text("712 345 678", color = colors.textSecondary) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.card,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                cursorColor = colors.primary,
            ),
        )
    }
}

private fun countryFor(phone: String): Country? =
    COUNTRIES.filter { phone.startsWith(it.dialCode) }.maxByOrNull { it.dialCode.length }

private val countrySaver = androidx.compose.runtime.saveable.Saver<Country, String>(
    save = { it.isoCode },
    restore = { iso -> COUNTRIES.firstOrNull { it.isoCode == iso } ?: COUNTRIES.first() },
)
