package africa.matchpass.sdk.internal

import android.content.Context
import android.content.SharedPreferences

internal class MatchPassStore(private val prefs: SharedPreferences) {

    internal constructor(context: Context) : this(
        context.getSharedPreferences("matchpass_passes", Context.MODE_PRIVATE)
    )

    fun savePass(contentId: String, token: String) =
        prefs.edit().putString("pass_$contentId", token).apply()

    fun getToken(contentId: String): String? =
        prefs.getString("pass_$contentId", null)

    fun clearPass(contentId: String) =
        prefs.edit()
            .remove("pass_$contentId")
            .remove("validated_at_$contentId")
            .remove("expires_at_$contentId")
            .apply()

    fun saveExpiresAt(contentId: String, epochMillis: Long) =
        prefs.edit().putLong("expires_at_$contentId", epochMillis).apply()

    fun getExpiresAt(contentId: String): Long =
        prefs.getLong("expires_at_$contentId", 0L)

    fun saveValidationTime(contentId: String, epochMillis: Long) =
        prefs.edit().putLong("validated_at_$contentId", epochMillis).apply()

    fun getValidationTime(contentId: String): Long =
        prefs.getLong("validated_at_$contentId", 0L)

    fun savePhone(phone: String) {
        val editor = prefs.edit()
        if (getPhone().isNotBlank() && getPhone() != phone) editor.remove("guest_session_token")
        editor.putString("last_phone", phone).apply()
    }

    fun saveVerifiedSession(phone: String, token: String) {
        require(token.isNotBlank()) { "Verified session token is required" }
        savePhone(phone)
        prefs.edit().putString("guest_session_token", token).apply()
    }

    fun getSessionToken(): String? = prefs.getString("guest_session_token", null)

    fun getPhone(): String =
        prefs.getString("last_phone", "") ?: ""

    fun clearPhone() =
        prefs.edit().remove("last_phone").remove("guest_session_token").apply()
}
