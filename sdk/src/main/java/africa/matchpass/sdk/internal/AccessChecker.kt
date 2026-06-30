package africa.matchpass.sdk.internal

import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassException
import africa.matchpass.sdk.MatchPassGrant
import retrofit2.HttpException
import java.io.IOException

internal class AccessChecker(
    private val config: MatchPassConfig,
    private val service: MatchPassService,
    private val store: MatchPassStore,
) {

    /**
     * Checks whether the user has a valid pass for [content].
     *
     * - If no token is stored → [AccessResult.NotPurchased].
     * - If a token is stored and the last successful validation is within the
     *   content's [PassPolicy.cacheTtlSeconds] → [AccessResult.Granted] without a
     *   server round-trip.
     * - Otherwise validates with the server. On success, updates the cache timestamp.
     * - On network errors, returns [AccessResult.Error] without clearing the stored
     *   pass (assume access rather than blocking the user on a transient failure).
     */
    suspend fun check(content: MatchPassContent): AccessResult {
        val token = store.getToken(content.id) ?: return AccessResult.NotPurchased

        // Respect the policy's cache window — skip server call when cache is fresh
        val cacheTtlMs = content.policy.cacheTtlSeconds * 1_000L
        val lastValidated = store.getValidationTime(content.id)
        if (lastValidated > 0L && System.currentTimeMillis() - lastValidated < cacheTtlMs) {
            return AccessResult.Granted(
                MatchPassGrant(token = token, contentId = content.id, expiresAt = "")
            )
        }

        return try {
            val dto = service.validatePass("ApiKey ${config.apiKey}", token)
            if (dto.isValid) {
                store.saveValidationTime(content.id, System.currentTimeMillis())
                AccessResult.Granted(
                    MatchPassGrant(token = token, contentId = content.id, expiresAt = dto.expiresAt)
                )
            } else {
                store.clearPass(content.id)
                when (dto.status) {
                    "revoked" -> AccessResult.Error(MatchPassException.PassRevoked())
                    else      -> AccessResult.Expired(dto.expiresAt)
                }
            }
        } catch (e: IOException) {
            AccessResult.Error(MatchPassException.NetworkError(e))
        } catch (e: HttpException) {
            AccessResult.Error(MatchPassException.ServerError(e.code(), e.message() ?: "Unknown error"))
        }
    }
}
