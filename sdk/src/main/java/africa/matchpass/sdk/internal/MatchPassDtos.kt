package africa.matchpass.sdk.internal

import com.google.gson.annotations.SerializedName
import africa.matchpass.sdk.MatchPassGrant

internal data class OtpRequestDto(@SerializedName("phone_number") val phoneNumber: String)

internal data class OtpResponseDto(
    val message: String = "",
    val otp: String = "",
)

internal data class OtpVerifyDto(
    @SerializedName("phone_number") val phoneNumber: String,
    val code: String,
)

internal data class GuestSessionDto(
    @SerializedName("session_token") val sessionToken: String = "",
    @SerializedName("user_ref") val userRef: String = "",
)

internal data class IssuePassDto(
    @SerializedName("content_id") val contentId: String,
    @SerializedName("user_ref") val userRef: String,
    val amount: String,
    val currency: String,
)

internal data class PassDto(
    val token: String = "",
    @SerializedName("content_id") val contentId: String = "",
    // Null for lifetime passes (movies/series) — Content.compute_pass_expiry()
    // returns None for those. Must stay nullable or Gson's reflection-based
    // construction sets an actual null into this field, tripping Kotlin's
    // non-null intrinsics check the moment it's read as a non-null String.
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("is_valid") val isValid: Boolean = false,
) {
    fun toGrant() = MatchPassGrant(token = token, contentId = contentId, expiresAt = expiresAt)
}

internal data class InitiatePaymentDto(
    @SerializedName("phone")         val phone: String,
    @SerializedName("content_id")    val contentId: String,
    @SerializedName("content_title") val contentTitle: String,
    @SerializedName("content_type")  val contentType: String,
    @SerializedName("user_ref")      val userRef: String,
    val amount: String,
    val currency: String,
)

internal data class InitiatePaymentResponseDto(
    @SerializedName("checkout_request_id") val checkoutRequestId: String = "",
)

internal data class PaymentStatusDto(
    val status: String = "pending",
    val token: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("result_desc") val resultDesc: String? = null,
)

internal data class LookupPassDto(
    val token: String = "",
    @SerializedName("content_id") val contentId: String = "",
    @SerializedName("expires_at") val expiresAt: String? = null,
    val valid: Boolean = false,
) {
    fun toGrant() = MatchPassGrant(token = token, contentId = contentId, expiresAt = expiresAt)
}

internal data class ValidatePassDto(
    @SerializedName("valid") val isValid: Boolean = false,
    val status: String = "",
    @SerializedName("expires_at") val expiresAt: String? = null,
    val reason: String? = null,
)
