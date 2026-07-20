package africa.matchpass.sdk.internal

import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassException
import africa.matchpass.sdk.MatchPassGrant
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class AccessChecker(
    private val config: MatchPassConfig,
    private val service: MatchPassService,
    private val store: MatchPassStore,
) {

    /**
     * Checks whether the user has a valid pass for [content].
     *
     * Order of checks:
     * 1. No token stored → NotPurchased.
     * 2. Stored expiry time is in the past → Expired (clears pass, no network).
     * 3. Last server validation is still within policy cache TTL → Granted (no network).
     * 4. Otherwise validate with the server. Caches new expiry and validation time.
     *    Network errors return Error without clearing the pass (fail open).
     */
    suspend fun check(content: MatchPassContent): AccessResult {
        val token = store.getToken(content.id) ?: return AccessResult.NotPurchased

        // Local expiry check — avoids a network round-trip for passes we know are done.
        val storedExpiry = store.getExpiresAt(content.id)
        if (storedExpiry > 0L && System.currentTimeMillis() >= storedExpiry) {
            store.clearPass(content.id)
            return AccessResult.Expired("")
        }

        // Cache window — respect policy TTL before hitting the server.
        val cacheTtlMs = content.policy.cacheTtlSeconds * 1_000L
        val lastValidated = store.getValidationTime(content.id)
        if (lastValidated > 0L && System.currentTimeMillis() - lastValidated < cacheTtlMs) {
            val expiresAtStr = if (storedExpiry > 0L) epochMillisToIso8601(storedExpiry) else null
            return AccessResult.Granted(
                MatchPassGrant(token = token, contentId = content.id, expiresAt = expiresAtStr)
            )
        }

        return try {
            val dto = service.validatePass("ApiKey ${config.apiKey}", token)
            if (dto.isValid) {
                store.saveValidationTime(content.id, System.currentTimeMillis())
                parseIso8601ToMillis(dto.expiresAt)?.let { store.saveExpiresAt(content.id, it) }
                AccessResult.Granted(
                    MatchPassGrant(token = token, contentId = content.id, expiresAt = dto.expiresAt)
                )
            } else {
                store.clearPass(content.id)
                when (dto.status) {
                    "revoked" -> AccessResult.Error(MatchPassException.PassRevoked())
                    else      -> AccessResult.Expired(dto.expiresAt ?: "")
                }
            }
        } catch (e: IOException) {
            AccessResult.Error(MatchPassException.NetworkError(e))
        } catch (e: HttpException) {
            AccessResult.Error(MatchPassException.ServerError(e.code(), e.message() ?: "Unknown error"))
        }
    }

    companion object {
        /**
         * Parses an ISO 8601 UTC string from DRF into epoch millis.
         * Handles microseconds ("2024-12-15T21:00:00.123456Z") by truncating to 3 decimal places.
         */
        internal fun epochMillisToIso8601(epochMillis: Long): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(epochMillis))

        internal fun parseIso8601ToMillis(dateStr: String?): Long? {
            if (dateStr.isNullOrBlank()) return null
            // DRF emits microseconds (6 digits after dot). SimpleDateFormat only handles millis (3).
            val normalized = dateStr.replace(Regex("\\.(\\d{3})\\d+Z$"), ".$1Z")
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            )
            for (fmt in formats) {
                try {
                    return SimpleDateFormat(fmt, Locale.US)
                        .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                        .parse(normalized)
                        ?.time
                } catch (_: Exception) {}
            }
            return null
        }
    }
}
