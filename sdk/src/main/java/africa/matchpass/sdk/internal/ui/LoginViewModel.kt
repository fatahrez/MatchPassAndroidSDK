package africa.matchpass.sdk.internal.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.MatchPassStore
import africa.matchpass.sdk.internal.OtpRequestDto
import africa.matchpass.sdk.internal.OtpVerifyDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginViewModel(
    private val config: MatchPassConfig,
    private val client: MatchPassClient,
    private val store: MatchPassStore,
    private val onLoggedIn: (phone: String) -> Unit,
) : ViewModel() {

    enum class Step { Phone, Otp }

    data class State(
        val step: Step = Step.Phone,
        val phone: String = "",
        val otp: String = "",
        val demoOtp: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        // Populated by an init-time lookup that runs in parallel with the
        // user typing their phone number, so it's ready by the time they
        // tap "Send code" without adding perceived latency. A single-entry
        // list (the common case) renders no picker at all.
        val availableChannels: List<String> = listOf("whatsapp"),
        val selectedChannel: String? = null,
    )

    private val _state = MutableStateFlow(State(phone = store.getPhone()))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { client.service.otpChannels(auth = "ApiKey ${config.apiKey}") }
                .onSuccess { res ->
                    _state.update { it.copy(availableChannels = res.channels, selectedChannel = res.default) }
                }
                // Silent — falls back to the single-channel default state above,
                // same as if the operator only had one channel enabled.
        }
    }

    fun setPhone(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun setOtp(v: String) = _state.update { it.copy(otp = v, error = null) }
    fun selectChannel(channel: String) = _state.update { it.copy(selectedChannel = channel) }
    fun goBack() = _state.update { it.copy(step = Step.Phone, otp = "", demoOtp = null, error = null) }

    fun requestOtp() {
        val phone = _state.value.phone.trim().ifBlank { return }
        val channel = _state.value.selectedChannel
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                client.service.requestOtp(auth = "ApiKey ${config.apiKey}", body = OtpRequestDto(phone, channel))
            }
                .onSuccess { res ->
                    _state.update {
                        it.copy(step = Step.Otp, isLoading = false, demoOtp = res.otp.ifBlank { null })
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun verifyOtp() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                client.service.verifyOtp(
                    auth = "ApiKey ${config.apiKey}",
                    body = OtpVerifyDto(phoneNumber = s.phone.trim(), code = s.otp.trim()),
                )
            }
                .onSuccess {
                    store.savePhone(s.phone.trim())
                    onLoggedIn(s.phone.trim())
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Incorrect OTP") }
                }
        }
    }

    class Factory(
        private val config: MatchPassConfig,
        private val client: MatchPassClient,
        private val context: Context,
        private val onLoggedIn: (String) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(config, client, MatchPassStore(context), onLoggedIn) as T
    }
}
