package africa.matchpass.sdk.internal.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassGrant
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
    private val onAccessGranted: (MatchPassGrant) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState(phoneNumber = store.getPhone()))
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    fun setPhone(value: String) = _state.update { it.copy(phoneNumber = value, error = null) }
    fun setOtp(value: String) = _state.update { it.copy(otpCode = value, error = null) }

    fun checkExistingPass() {
        val token = store.getToken(content.id) ?: return
        viewModelScope.launch {
            _state.update { it.copy(step = PaywallStep.Resuming) }
            val valid = runCatching {
                client.service.validatePass("ApiKey ${config.apiKey}", token).isValid
            }.getOrDefault(false)
            if (valid) {
                onAccessGranted(MatchPassGrant(token = token, contentId = content.id, expiresAt = ""))
            } else {
                store.clearPass(content.id)
                _state.update {
                    it.copy(
                        step = PaywallStep.EnteringPhone,
                        error = "Your pass has expired. Please purchase a new one.",
                    )
                }
            }
        }
    }

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

    fun confirmAndPay() {
        val s = _state.value
        viewModelScope.launch {
            // 1. Simulate payment processing
            _state.update { it.copy(step = PaywallStep.ProcessingPayment, error = null) }
            delay(2600)

            // 2. Issue pass
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

            // 3. Poll until confirmed active
            _state.update { it.copy(step = PaywallStep.Polling) }
            repeat(5) { attempt ->
                delay(700)
                val valid = runCatching {
                    client.service.validatePass("ApiKey ${config.apiKey}", passDto.token).isValid
                }.getOrDefault(false)
                if (valid || attempt == 4) {
                    _state.update { it.copy(step = PaywallStep.AccessGranted) }
                    onAccessGranted(passDto.toGrant())
                    return@launch
                }
            }
        }
    }

    class Factory(
        private val config: MatchPassConfig,
        private val content: MatchPassContent,
        private val client: MatchPassClient,
        private val context: Context,
        private val onAccessGranted: (MatchPassGrant) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PaywallViewModel(config, content, client, MatchPassStore(context), onAccessGranted) as T
    }
}
