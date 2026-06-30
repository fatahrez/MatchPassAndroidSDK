package africa.matchpass.sdk.internal

import android.content.Context

internal class MatchPassStore(context: Context) {
    private val prefs = context.getSharedPreferences("matchpass_passes", Context.MODE_PRIVATE)

    fun savePass(contentId: String, token: String) =
        prefs.edit().putString("pass_$contentId", token).apply()

    fun getToken(contentId: String): String? =
        prefs.getString("pass_$contentId", null)

    fun clearPass(contentId: String) =
        prefs.edit().remove("pass_$contentId").apply()

    fun savePhone(phone: String) =
        prefs.edit().putString("last_phone", phone).apply()

    fun getPhone(): String =
        prefs.getString("last_phone", "") ?: ""
}
