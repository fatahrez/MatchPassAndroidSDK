package africa.matchpass.sdk.internal

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

internal interface MatchPassService {

    @POST("guests/otp/request/")
    suspend fun requestOtp(
        @Header("Authorization") auth: String,
        @Query("demo") demo: Boolean = false,
        @Body body: OtpRequestDto,
    ): OtpResponseDto

    @POST("guests/otp/verify/")
    suspend fun verifyOtp(
        @Header("Authorization") auth: String,
        @Body body: OtpVerifyDto,
    ): GuestSessionDto

    @POST("passes/issue/")
    suspend fun issuePass(
        @Header("Authorization") auth: String,
        @Body body: IssuePassDto,
    ): PassDto

    @GET("passes/validate/")
    suspend fun validatePass(
        @Header("Authorization") auth: String,
        @Query("token") token: String,
    ): ValidatePassDto

    /**
     * Looks up an existing valid pass by user_ref + content_id.
     * Returns 404 if no valid pass exists — caller should catch [HttpException] with code 404
     * and proceed to the purchase flow.
     */
    @POST("payments/initiate/")
    suspend fun initiatePayment(
        @Header("Authorization") auth: String,
        @Body body: InitiatePaymentDto,
    ): InitiatePaymentResponseDto

    @GET("payments/status/{checkout_request_id}/")
    suspend fun paymentStatus(
        @Header("Authorization") auth: String,
        @retrofit2.http.Path("checkout_request_id") checkoutRequestId: String,
    ): PaymentStatusDto

    @GET("passes/lookup/")
    suspend fun lookupPass(
        @Header("Authorization") auth: String,
        @Query("user_ref") userRef: String,
        @Query("content_id") contentId: String,
    ): LookupPassDto
}
