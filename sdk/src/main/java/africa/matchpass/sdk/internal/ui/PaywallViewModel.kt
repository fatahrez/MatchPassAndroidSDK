package africa.matchpass.sdk.internal.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassGrant
import africa.matchpass.sdk.internal.AccessChecker
import africa.matchpass.sdk.internal.IssuePassDto
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.MatchPassStore
import africa.matchpass.sdk.internal.OtpRequestDto
import africa.matchpass.sdk.internal.OtpVerifyDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PaywallViewModel(
    private val config: MatchPassConfig,
    private val content: MatchPassContent,
    private val client: MatchPassClient,
    private val store: MatchPassStore,
    private val checker: AccessChecker,
    private val onAccessGranted: (MatchPassGrant) -> Unit,
    /** Pre-authenticated phone from the host app — skips the OTP step entirely. */
    private val userPhone: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PaywallState(phoneNumber = userPhone ?: store.getPhone())
    )
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    fun setPhone(value: String) = _state.update { it.copy(phoneNumber = value, error = null) }
    fun setOtp(value: String) = _state.update { it.copy(otpCode = value, error = null) }
    fun changePhone() = _state.update { it.copy(step = PaywallStep.EnteringPhone, error = null) }

    /**
     * Called when the paywall opens. Three outcomes:
     * 1. Existing pass found → validate silently → fire [onAccessGranted] if still valid.
     * 2. No pass, but phone is known (logged in previously or provided by host app) →
     *    skip OTP and go straight to the purchase confirmation screen.
     * 3. First time user → show phone entry to initiate OTP login.
     */
    fun onStart() {
        val hasExistingPass = store.getToken(content.id) != null
        val knownPhone = userPhone ?: store.getPhone().ifBlank { null }

        when {
            hasExistingPass -> resumeExistingPass()
            knownPhone != null -> {
                if (userPhone != null) store.savePhone(userPhone)
                _state.update { it.copy(step = PaywallStep.Confirming) }
            }
            else -> _state.update { it.copy(step = PaywallStep.EnteringPhone) }
        }
    }

    private fun resumeExistingPass() {
        viewModelScope.launch {
            _state.update { it.copy(step = PaywallStep.Resuming) }
            when (val result = checker.check(content)) {
                is AccessResult.Granted -> onAccessGranted(result.grant)
                is AccessResult.Expired -> _state.update {
                    it.copy(step = PaywallStep.Confirming, error = "Your pass expired. Please purchase a new one.")
                }
                is AccessResult.NotPurchased -> _state.update { it.copy(step = PaywallStep.Confirming) }
                is AccessResult.Error -> _state.update {
                    it.copy(step = PaywallStep.Confirming, error = result.exception.message)
                }
            }
        }
    }

    // ── OTP login (first-time users only) ────────────────────────────────────

    fun requestOtp() {
        val phone = _state.value.phoneNumber.trim().ifBlank { return }
        store.savePhone(phone)
        viewModelScope.launch {
            _state.update { it.copy(step = PaywallStep.AwaitingOtp, error = null) }
            runCatching { client.service.requestOtp(body = OtpRequestDto(phone)) }
                .onSuccess { res -> _state.update { it.copy(demoOtp = res.otp.ifBlank { null }) } }
                .onFailure { e ->
                    _state.update { it.copy(step = PaywallStep.EnteringPhone, error = e.message) }
                }
        }
    }

    fun verifyOtp() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                client.service.verifyOtp(OtpVerifyDto(phoneNumber = s.phoneNumber.trim(), code = s.otpCode.trim()))
            }
                .onSuccess { _state.update { it.copy(step = PaywallStep.Confirming) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Incorrect OTP") } }
        }
    }

    // ── Purchase ─────────────────────────────────────────────────────────────

    fun confirmAndPay() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(step = PaywallStep.ProcessingPayment, error = null) }
            delay(2_600)

            _state.update { it.copy(step = PaywallStep.Issuing) }
            val passDto = runCatching {
                client.service.issuePass(
                    auth = "ApiKey ${config.apiKey}",
                    body = IssuePassDto(
                        contentId = content.id,
                        userRef = s.phoneNumber.trim(),
                        amount = content.price,
                        currency = content.currency,
                    ),
                )
            }.getOrElse { e ->
                _state.update { it.copy(step = PaywallStep.Confirming, error = e.message) }
                return@launch
            }

            store.savePass(content.id, passDto.token)

            _state.update { it.copy(step = PaywallStep.Polling) }
            repeat(5) { attempt ->
                delay(700)
                val valid = runCatching {
                    client.service.validatePass("ApiKey ${config.apiKey}", passDto.token).isValid
                }.getOrDefault(false)
                if (valid || attempt == 4) {
                    store.saveValidationTime(content.id, System.currentTimeMillis())
                    _state.update { it.copy(step = PaywallStep.AccessGranted, issuedGrant = passDto.toGrant()) }
                    return@launch
                }
            }
        }
    }

    /** Called when the user taps "Watch Now" on the AccessGrantedPanel. */
    fun watchContent() {
        _state.value.issuedGrant?.let { onAccessGranted(it) }
    }

    class Factory(
        private val config: MatchPassConfig,
        private val content: MatchPassContent,
        private val client: MatchPassClient,
        private val context: Context,
        private val onAccessGranted: (MatchPassGrant) -> Unit,
        private val userPhone: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val store = MatchPassStore(context)
            val checker = AccessChecker(config, client.service, store)
            return PaywallViewModel(config, content, client, store, checker, onAccessGranted, userPhone) as T
        }
    }
}
