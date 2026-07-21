package africa.matchpass.sdk.internal.ui

import africa.matchpass.sdk.MatchPassGrant

internal enum class PaywallStep {
    Resuming,
    EnteringPhone,
    AwaitingOtp,
    Confirming,
    ChangingPaymentPhone,   // user wants to pay with a different number — no OTP needed
    ProcessingPayment,
    Issuing,
    Polling,
    AccessGranted,
}

internal data class PaywallState(
    val step: PaywallStep = PaywallStep.EnteringPhone,
    val phoneNumber: String = "",       // login identity (OTP-verified)
    val paymentPhone: String? = null,   // M-Pesa target; null → falls back to phoneNumber
    val editingPaymentPhone: String = "", // transient while in ChangingPaymentPhone step
    val otpCode: String = "",
    val demoOtp: String? = null,
    // See LoginViewModel.State — same init-time lookup, same picker gating.
    val availableChannels: List<String> = listOf("whatsapp"),
    val selectedChannel: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val issuedGrant: MatchPassGrant? = null,
)
