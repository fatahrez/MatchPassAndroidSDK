package africa.matchpass.sdk.internal.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.ContentType
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassGrant
import africa.matchpass.sdk.internal.AccessChecker
import africa.matchpass.sdk.internal.InitiatePaymentDto
import africa.matchpass.sdk.internal.MatchPassClient
import africa.matchpass.sdk.internal.MatchPassStore
import africa.matchpass.sdk.internal.OtpRequestDto
import africa.matchpass.sdk.internal.OtpVerifyDto
import africa.matchpass.sdk.internal.toFriendlyMessage
import retrofit2.HttpException
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

    // ── Payment phone (no OTP — just changes the M-Pesa target) ─────────────
    fun changePaymentPhone() = _state.update {
        it.copy(
            step = PaywallStep.ChangingPaymentPhone,
            editingPaymentPhone = it.paymentPhone ?: it.phoneNumber,
            error = null,
        )
    }
    fun setEditingPaymentPhone(value: String) =
        _state.update { it.copy(editingPaymentPhone = value, error = null) }
    fun confirmPaymentPhone() = _state.update {
        it.copy(
            step = PaywallStep.Confirming,
            paymentPhone = it.editingPaymentPhone.trim().ifBlank { null },
            error = null,
        )
    }
    fun cancelPaymentPhoneChange() = _state.update {
        it.copy(step = PaywallStep.Confirming, error = null)
    }

    /**
     * Called when the paywall opens. Four outcomes:
     * 1. Local token found → validate silently → fire [onAccessGranted] if still valid.
     * 2. No local token, phone is known → check server for an existing pass (restore
     *    after reinstall / device switch). If found → restore locally + grant access.
     * 3. No local token, phone known, no server pass → show purchase confirmation.
     * 4. First-time user (no phone) → show phone entry for OTP login.
     */
    fun onStart() {
        val hasExistingPass = store.getToken(content.id) != null
        val knownPhone = userPhone ?: store.getPhone().ifBlank { null }

        when {
            hasExistingPass -> resumeExistingPass()
            knownPhone != null -> {
                if (userPhone != null) store.savePhone(userPhone)
                lookupServerPass(knownPhone)
            }
            else -> _state.update { it.copy(step = PaywallStep.EnteringPhone) }
        }
    }

    /**
     * Checks the server for a valid pass this user already purchased (another device,
     * reinstall, etc). On success the pass is restored locally and access is granted
     * without any payment. On 404 the user is sent to the purchase confirmation screen.
     */
    private fun lookupServerPass(phone: String) {
        viewModelScope.launch {
            _state.update { it.copy(step = PaywallStep.Resuming) }
            try {
                val dto = client.service.lookupPass(
                    auth = "ApiKey ${config.apiKey}",
                    userRef = phone,
                    contentId = content.id,
                )
                // Restore pass locally so subsequent opens skip this round-trip
                store.savePass(content.id, dto.token)
                store.saveValidationTime(content.id, System.currentTimeMillis())
                AccessChecker.parseIso8601ToMillis(dto.expiresAt)
                    ?.let { store.saveExpiresAt(content.id, it) }
                onAccessGranted(dto.toGrant())
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    // No pass on server — safe to proceed to purchase
                    _state.update { it.copy(step = PaywallStep.Confirming) }
                } else {
                    _state.update {
                        it.copy(
                            step = PaywallStep.Confirming,
                            error = "Could not verify your pass (${e.code()}). Tap Pay to purchase.",
                        )
                    }
                }
            } catch (e: Exception) {
                // Network error — go to Confirming with a warning so they can retry
                _state.update {
                    it.copy(
                        step = PaywallStep.Confirming,
                        error = "Network error checking your pass. Check your connection.",
                    )
                }
            }
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
            _state.update { it.copy(error = null, isLoading = true) }
            runCatching { client.service.requestOtp(auth = "ApiKey ${config.apiKey}", body = OtpRequestDto(phone)) }
                .onSuccess { res ->
                    _state.update {
                        it.copy(step = PaywallStep.AwaitingOtp, isLoading = false, demoOtp = res.otp.ifBlank { null })
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.toFriendlyMessage()) }
                }
        }
    }

    fun verifyOtp() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                client.service.verifyOtp(
                    auth = "ApiKey ${config.apiKey}",
                    body = OtpVerifyDto(phoneNumber = s.phoneNumber.trim(), code = s.otpCode.trim()),
                )
            }
                .onSuccess { _state.update { it.copy(step = PaywallStep.Confirming) } }
                .onFailure { e ->
                    // A 400 here almost always means the code was wrong or expired —
                    // more specific than the generic mapper's message for this call site.
                    val message = if ((e as? HttpException)?.code() == 400) {
                        "The OTP code is incorrect or has expired. Please check and try again."
                    } else {
                        e.toFriendlyMessage()
                    }
                    _state.update { it.copy(error = message) }
                }
        }
    }

    // ── Purchase (M-Pesa STK Push) ────────────────────────────────────────────

    fun confirmAndPay() {
        val s = _state.value
        // Use the overridden payment phone if set, otherwise fall back to login phone
        val stkPhone = normalizePhone((s.paymentPhone ?: s.phoneNumber).trim())
        viewModelScope.launch {
            // Step 1 — send STK Push
            _state.update { it.copy(step = PaywallStep.ProcessingPayment, error = null) }
            val initDto = runCatching {
                client.service.initiatePayment(
                    auth = "ApiKey ${config.apiKey}",
                    body = InitiatePaymentDto(
                        phone        = stkPhone,
                        contentId    = content.id,
                        contentTitle = content.title,
                        contentType  = if (content.contentType == ContentType.SEASON) "series_season" else content.contentType.name.lowercase(),
                        userRef      = s.phoneNumber.trim(), // identity always uses login phone
                        amount       = content.price,
                        currency     = content.currency,
                    ),
                )
            }.getOrElse { e ->
                _state.update { it.copy(step = PaywallStep.Confirming, error = "Could not send payment request. ${e.toFriendlyMessage()}") }
                return@launch
            }

            // Step 2 — poll for payment completion (30 × 3s = 90s timeout)
            _state.update { it.copy(step = PaywallStep.Polling) }
            repeat(30) {
                delay(3_000)
                val statusDto = runCatching {
                    client.service.paymentStatus("ApiKey ${config.apiKey}", initDto.checkoutRequestId)
                }.getOrNull() ?: return@repeat

                when (statusDto.status) {
                    "completed" -> {
                        val token = statusDto.token ?: return@repeat
                        store.savePass(content.id, token)
                        store.saveValidationTime(content.id, System.currentTimeMillis())
                        statusDto.expiresAt?.let { AccessChecker.parseIso8601ToMillis(it) }
                            ?.let { store.saveExpiresAt(content.id, it) }
                        val grant = africa.matchpass.sdk.MatchPassGrant(token, content.id, statusDto.expiresAt)
                        _state.update { it.copy(step = PaywallStep.AccessGranted, issuedGrant = grant) }
                        return@launch
                    }
                    "failed", "cancelled", "timed_out" -> {
                        _state.update { it.copy(step = PaywallStep.Confirming, error = statusDto.resultDesc ?: "Payment failed. Please try again.") }
                        return@launch
                    }
                    // "pending" — keep polling
                }
            }

            // 90 seconds elapsed with no result
            _state.update { it.copy(step = PaywallStep.Confirming, error = "Payment timed out. Please try again.") }
        }
    }

    private fun normalizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("254") -> digits
            digits.startsWith("0")   -> "254${digits.substring(1)}"
            else                     -> digits
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
