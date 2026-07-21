package africa.matchpass.sdk.internal.ui

internal fun otpChannelLabel(channel: String): String = when (channel) {
    "sms" -> "SMS"
    "on_screen" -> "On-screen"
    else -> "WhatsApp"
}
