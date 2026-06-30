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
    @SerializedName("expires_at") val expiresAt: String = "",
    @SerializedName("is_valid") val isValid: Boolean = false,
) {
    fun toGrant() = MatchPassGrant(token = token, contentId = contentId, expiresAt = expiresAt)
}

internal data class ValidatePassDto(
    @SerializedName("valid") val isValid: Boolean = false,
    val status: String = "",
)
