package africa.matchpass.sdk.internal

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

internal interface MatchPassService {

    @POST("guests/otp/request/")
    suspend fun requestOtp(
        @Query("demo") demo: Boolean = true,
        @Body body: OtpRequestDto,
    ): OtpResponseDto

    @POST("guests/otp/verify/")
    suspend fun verifyOtp(@Body body: OtpVerifyDto): GuestSessionDto

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
    @GET("passes/lookup/")
    suspend fun lookupPass(
        @Header("Authorization") auth: String,
        @Query("user_ref") userRef: String,
        @Query("content_id") contentId: String,
    ): LookupPassDto
}
