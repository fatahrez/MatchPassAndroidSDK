package africa.matchpass.sdk.internal.ui

import africa.matchpass.sdk.MatchPassGrant

internal enum class PaywallStep {
    Resuming,
    EnteringPhone,
    AwaitingOtp,
    Confirming,
    ProcessingPayment,
    Issuing,
    Polling,
    AccessGranted,
}

internal data class PaywallState(
    val step: PaywallStep = PaywallStep.EnteringPhone,
    val phoneNumber: String = "",
    val otpCode: String = "",
    val demoOtp: String? = null,
    val error: String? = null,
    val issuedGrant: MatchPassGrant? = null,
)
