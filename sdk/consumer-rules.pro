# MatchPass Android SDK — consumer Proguard rules
# These are applied automatically to any app that depends on this SDK.

# Public API surface — keep all public classes and members
-keep class africa.matchpass.sdk.MatchPassSDK { *; }
-keep class africa.matchpass.sdk.MatchPassSDK$Builder { *; }
-keep class africa.matchpass.sdk.MatchPassConfig { *; }
-keep class africa.matchpass.sdk.MatchPassContent { *; }
-keep class africa.matchpass.sdk.MatchPassGrant { *; }
-keep class africa.matchpass.sdk.ContentType { *; }
-keep class africa.matchpass.sdk.PassPolicy { *; }
-keep class africa.matchpass.sdk.AccessResult { *; }
-keep class africa.matchpass.sdk.AccessResult$* { *; }
-keep class africa.matchpass.sdk.MatchPassException { *; }
-keep class africa.matchpass.sdk.MatchPassException$* { *; }

# Gson DTOs — serialized field names must not be renamed
-keepclassmembers class africa.matchpass.sdk.internal.**Dto {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit — keep service interface method signatures
-keep interface africa.matchpass.sdk.internal.MatchPassService { *; }

# Kotlin data classes — keep component functions and copy for public types
-keepclassmembers class africa.matchpass.sdk.MatchPassContent {
    public synthetic <methods>;
}
-keepclassmembers class africa.matchpass.sdk.MatchPassGrant {
    public synthetic <methods>;
}
-keepclassmembers class africa.matchpass.sdk.PassPolicy {
    public synthetic <methods>;
}
