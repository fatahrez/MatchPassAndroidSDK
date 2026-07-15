package africa.matchpass.sdk.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
        Row(
            modifier = Modifier
                .widthIn(min = 84.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.card)
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(10.dp))
            Text(selectedCountry.dialCode, color = colors.text, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose country", tint = colors.textSecondary)

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
