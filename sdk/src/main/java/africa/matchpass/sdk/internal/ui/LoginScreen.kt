package africa.matchpass.sdk.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import africa.matchpass.sdk.internal.MatchPassClient

@Composable
internal fun LoginScreen(
    client: MatchPassClient,
    onLoggedIn: (phone: String) -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val vm: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(client, context, onLoggedIn)
    )
    val state by vm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D1B4B), SdkColors.background))
            ),
    ) {
        when (state.step) {
            LoginViewModel.Step.Phone -> PhoneLoginStep(
                state = state,
                onPhoneChange = vm::setPhone,
                onRequestOtp = vm::requestOtp,
                onSkip = onSkip,
            )
            LoginViewModel.Step.Otp -> OtpLoginStep(
                state = state,
                onOtpChange = vm::setOtp,
                onVerify = vm::verifyOtp,
                onBack = vm::goBack,
            )
        }
    }
}

// ── Phone step ─────────────────────────────────────────────────────────────────

@Composable
private fun PhoneLoginStep(
    state: LoginViewModel.State,
    onPhoneChange: (String) -> Unit,
    onRequestOtp: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Branding
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = SdkColors.gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "MatchPass",
                color = SdkColors.gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Watch what you want.\nPay only for what you watch.",
            color = SdkColors.text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 32.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in once with your mobile number. No subscription, no account — just instant access.",
            color = SdkColors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(40.dp))

        Text(
            text = "Mobile number",
            color = SdkColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.phone,
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
        state.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = SdkColors.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequestOtp,
            enabled = state.phone.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SdkColors.blue),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("Send verification code", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Skip for now — browse without signing in",
                color = SdkColors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── OTP step ───────────────────────────────────────────────────────────────────

@Composable
private fun OtpLoginStep(
    state: LoginViewModel.State,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SdkColors.text)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Verify your number", color = SdkColors.text, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "We sent a 6-digit code to ${state.phone}",
                color = SdkColors.textSecondary,
                fontSize = 14.sp,
            )

            // Demo OTP hint card
            state.demoOtp?.let { otp ->
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22F5A623))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Demo code", color = SdkColors.gold, fontSize = 12.sp)
                    Text(otp, color = SdkColors.gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.otp,
                onValueChange = { if (it.length <= 6) onOtpChange(it) },
                placeholder = { Text("6-digit code", color = SdkColors.textSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SdkColors.blue,
                    unfocusedBorderColor = SdkColors.card,
                    focusedTextColor = SdkColors.text,
                    unfocusedTextColor = SdkColors.text,
                    cursorColor = SdkColors.blue,
                ),
            )
            state.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = SdkColors.error, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onVerify,
                enabled = state.otp.length == 6 && !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SdkColors.blue),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Verify & Sign in", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Use a different number", color = SdkColors.textSecondary, fontSize = 13.sp)
            }
        }
    }
}
