package africa.matchpass.sdk.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassGrant
import africa.matchpass.sdk.MatchPassSDK
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.ui.panels.AccessGrantedPanel
import africa.matchpass.sdk.internal.ui.panels.ConfirmPanel
import africa.matchpass.sdk.internal.ui.panels.IssuingPanel
import africa.matchpass.sdk.internal.ui.panels.OtpPanel
import africa.matchpass.sdk.internal.ui.panels.PaymentPanel
import africa.matchpass.sdk.internal.ui.panels.PhonePanel

@Composable
internal fun PaywallScreen(
    content: MatchPassContent,
    client: MatchPassClient,
    onAccessGranted: (MatchPassGrant) -> Unit,
    onDismiss: () -> Unit,
    /** Pre-authenticated phone — skips OTP if provided. */
    userPhone: String? = null,
) {
    val context = LocalContext.current
    val config = MatchPassSDK.config
    // key = content.id ensures a separate ViewModel per content item.
    // Without this, viewModel() returns the same instance regardless of which
    // content is shown (typed by class only), causing stale state on repeated opens.
    val vm: PaywallViewModel = viewModel(
        key = content.id,
        factory = PaywallViewModel.Factory(config, content, client, context, onAccessGranted, userPhone),
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(content.id) {
        vm.onStart()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Consume touches on the dim area so they don't fall through to content below
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SdkColors.overlay)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            when (state.step) {
                PaywallStep.Resuming          -> IssuingPanel("Checking your pass...")
                PaywallStep.Issuing           -> IssuingPanel("Activating your pass...")
                PaywallStep.Polling           -> IssuingPanel("Confirming payment...")
                PaywallStep.ProcessingPayment -> PaymentPanel(content = content, state = state)
                PaywallStep.AccessGranted     -> AccessGrantedPanel(
                    content = content,
                    onWatch = vm::watchContent,
                )
                PaywallStep.Confirming        -> ConfirmPanel(
                    content = content,
                    state = state,
                    onConfirm = vm::confirmAndPay,
                    onChangePhone = vm::changePhone,
                    onDismiss = onDismiss,
                )
                PaywallStep.AwaitingOtp       -> OtpPanel(
                    state = state,
                    onOtpChange = vm::setOtp,
                    onVerify = vm::verifyOtp,
                    onDismiss = onDismiss,
                )
                PaywallStep.EnteringPhone     -> PhonePanel(
                    content = content,
                    state = state,
                    onPhoneChange = vm::setPhone,
                    onRequestOtp = vm::requestOtp,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}
