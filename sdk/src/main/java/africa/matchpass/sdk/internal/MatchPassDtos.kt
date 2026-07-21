package africa.matchpass.sdk.internal

import com.google.gson.annotations.SerializedName
import africa.matchpass.sdk.MatchPassGrant

internal data class OtpRequestDto(
    @SerializedName("phone_number") val phoneNumber: String,
    // Omit to let the backend use the operator's default channel. Must be
    // one of the values returned by OtpChannelsDto.channels for this
    // operator, or the request is rejected with a 400.
    val channel: String? = null,
)

internal data class OtpResponseDto(
    val message: String = "",
    val otp: String = "",
)

internal data class OtpChannelsDto(
    val channels: List<String> = listOf("whatsapp"),
    val default: String = "whatsapp",
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

internal data class PassSummaryDto(
    @SerializedName("content_id")    val contentId: String = "",
    @SerializedName("content_title") val contentTitle: String = "",
    @SerializedName("content_type")  val contentType: String = "",
    val status: String = "",
    val amount: String = "",
    val currency: String = "",
    @SerializedName("issued_at")     val issuedAt: String = "",
    @SerializedName("expires_at")    val expiresAt: String? = null,
) {
    fun toOwnedItem() = africa.matchpass.sdk.MatchPassOwnedItem(
        contentId = contentId,
        title = contentTitle,
        contentType = contentType,
        amount = amount,
        currency = currency,
        issuedAt = issuedAt,
        expiresAt = expiresAt,
    )
}

internal data class PassListResponseDto(
    val count: Int = 0,
    val next: String? = null,
    val results: List<PassSummaryDto> = emptyList(),
)
