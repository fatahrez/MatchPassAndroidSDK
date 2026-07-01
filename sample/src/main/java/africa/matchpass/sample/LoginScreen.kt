package africa.matchpass.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.MatchPassSDK

// ── Demo credentials ───────────────────────────────────────────────────────────
// Change these to whatever you want for your demo.
// The demo phone is stored via MatchPassSDK.setPhone() so the paywall
// recognises the user as returning and skips the OTP step.
private const val DEMO_USERNAME = "demo"
private const val DEMO_PASSWORD = "play"
private const val DEMO_PHONE    = "0712345678"   // pre-fills paywall for the demo

/**
 * Operator-branded login screen for the StreamPlay sample app.
 *
 * Two login paths:
 *  1. Demo credentials — skips OTP, sets a demo phone via [MatchPassSDK.setPhone]
 *     so the paywall shows "Confirming" immediately. Requires the SDK to compile.
 *  2. "Login with phone" — fires the MatchPass OTP screen directly.
 *     Also requires the SDK. Remove the SDK and neither path works.
 *
 * [onLoggedIn] is called with the verified phone once either path succeeds.
 */
@Composable
fun LoginScreen(onLoggedIn: (phone: String) -> Unit) {
    val context = LocalContext.current

    // When true, the full MatchPass OTP screen covers this screen
    var showingPhoneLogin by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ── MatchPass OTP login overlay ────────────────────────────────────────────
    // This composable is powered entirely by the SDK. Comment out the SDK and
    // this block does not compile — neither does the demo login path below.
    if (showingPhoneLogin) {
        MatchPassSDK.Login(
            onLoggedIn = { phone ->
                showingPhoneLogin = false
                onLoggedIn(phone)
            },
            onSkip = { showingPhoneLogin = false },
        )
        return
    }

    // ── Operator's own login UI ────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF0D1B4B)))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand),
                contentAlignment = Alignment.Center,
            ) {
                Text("S", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("StreamPlay", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Your sport. Your movies. Your way.", color = Color(0xFF8A9BB8), fontSize = 13.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(48.dp))

            // ── Credentials form ───────────────────────────────────────────────
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMsg = null },
                placeholder = { Text("Username", color = Color(0xFF8A9BB8)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Brand,
                    unfocusedBorderColor = Color(0xFF2A3040),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = Brand,
                ),
                singleLine = true,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                placeholder = { Text("Password", color = Color(0xFF8A9BB8)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Brand,
                    unfocusedBorderColor = Color(0xFF2A3040),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = Brand,
                ),
                singleLine = true,
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMsg!!, color = Color(0xFFE05252), fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (username.trim() == DEMO_USERNAME && password == DEMO_PASSWORD) {
                        // Requires SDK — won't compile if SDK is removed
                        MatchPassSDK.setPhone(context, DEMO_PHONE)
                        onLoggedIn(DEMO_PHONE)
                    } else {
                        errorMsg = "Incorrect username or password."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
            ) {
                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(32.dp))

            // ── Divider ────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A3040))
                Text("  or  ", color = Color(0xFF8A9BB8), fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A3040))
            }

            Spacer(Modifier.height(24.dp))

            // ── SDK-powered phone login ────────────────────────────────────────
            // This button requires MatchPassSDK to be present. Without the SDK
            // dependency, this code does not compile and there is no way to log in.
            Button(
                onClick = { showingPhoneLogin = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color(0xFF0057FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D1B4B),
                    contentColor   = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Brand,
                )
                Spacer(Modifier.width(10.dp))
                Text("Login with phone number", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Hint for demo
            Text(
                text = "Demo: username \"demo\", password \"play\"",
                color = Color(0xFF8A9BB8).copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}
