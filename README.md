# MatchPass Android SDK

Pay-per-view access infrastructure for Android apps.  
Drop in one composable. MatchPass handles OTP auth, pass issuance, entitlement validation, and local persistence — your app just receives a token and starts the stream.

---

## How it works

```
Your app                      MatchPass SDK                    MatchPass API
────────                      ─────────────                    ─────────────
User taps Watch ──────────▶  Check stored pass
                              (if valid) ──────────────────────▶ /passes/validate
                              (if expired/none)
                              Show phone entry UI
                              User enters number ──────────────▶ /guests/otp/request
                              Show OTP entry UI
                              User enters code ────────────────▶ /guests/otp/verify
                              Show confirm screen
                              User confirms ───────────────────▶ /passes/issue
                              Poll until active ───────────────▶ /passes/validate
                ◀─────────── onAccessGranted(grant)
Start stream ◀──────────────  (grant.token → your backend)
```

The entire flow above is contained in a single composable: `MatchPassSDK.Paywall(...)`.  
On subsequent opens, the stored pass is validated silently — the user goes straight to the stream.

---

## Requirements

- Android minSdk 24
- Jetpack Compose (BOM 2024.09.00 or later)
- Kotlin 2.0+

---

## Installation

### JitPack

Add JitPack to your project-level `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app module:

```kotlin
dependencies {
    implementation("com.github.MatchPassAfrica:MatchPassAndroidSDK:0.1.0")
}
```

### Local (development)

Clone this repo, then add it as a local module in your `settings.gradle.kts`:

```kotlin
include(":matchpass-sdk")
project(":matchpass-sdk").projectDir = File("../MatchPassAndroidSDK/sdk")
```

Then depend on it:

```kotlin
dependencies {
    implementation(project(":matchpass-sdk"))
}
```

---

## Setup

Initialise the SDK once in your `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MatchPassSDK.init(
            context = this,
            config = MatchPassConfig(
                apiKey  = "your-operator-api-key",   // from the MatchPass dashboard
                baseUrl = "https://api.matchpass.africa/api/v1/",
                debug   = false,
            ),
        )
    }
}
```

**Never call `init()` more than once.** Subsequent calls are ignored.

---

## Usage

### Show the paywall

Replace your existing subscriber wall with `MatchPassSDK.Paywall`:

```kotlin
@Composable
fun ContentScreen(contentId: String, onStartStream: (token: String) -> Unit) {
    var showPaywall by remember { mutableStateOf(false) }

    if (showPaywall) {
        MatchPassSDK.Paywall(
            content = MatchPassContent(
                id           = contentId,
                title        = "Arsenal vs Man City",
                price        = "29.00",
                currency     = "ZAR",
                durationHours = 4,
                thumbnailUrl = "https://your-cdn.com/thumbnail.jpg",
            ),
            onAccessGranted = { grant ->
                showPaywall = false
                onStartStream(grant.token)   // pass token to your streaming backend
            },
            onDismiss = { showPaywall = false },
        )
    } else {
        WatchButton(onClick = { showPaywall = true })
    }
}
```

The paywall is self-managing:
- **First open** — runs the full OTP → payment → issuance flow.
- **Subsequent opens** — validates the stored pass silently. If still valid, `onAccessGranted` fires immediately with no UI shown.
- **Expired pass** — clears local storage, shows an error, restarts the purchase flow.

---

### Check access without showing UI

Useful for showing a "Resume" button instead of a "Watch" button when the user already has a pass:

```kotlin
LaunchedEffect(contentId) {
    val hasAccess = MatchPassSDK.hasAccess(context, contentId)
    if (hasAccess) showResumeButton() else showWatchButton()
}
```

This validates against the server on every call — do not call it in a tight loop.

---

## API reference

### `MatchPassConfig`

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `apiKey` | `String` | ✓ | — | Your operator API key from the MatchPass dashboard |
| `baseUrl` | `String` | | `https://api.matchpass.africa/api/v1/` | Override for staging or self-hosted |
| `debug` | `Boolean` | | `false` | Enables full OkHttp request/response logging |

---

### `MatchPassContent`

Describes the content the user is trying to access. Use your own IDs — MatchPass never needs to know your content schema.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `id` | `String` | ✓ | — | Your content identifier (must match what you registered in the MatchPass dashboard) |
| `title` | `String` | ✓ | — | Displayed on the paywall confirm screen |
| `price` | `String` | ✓ | — | Amount as decimal string, e.g. `"29.00"` |
| `currency` | `String` | | `"ZAR"` | ISO 4217 code |
| `durationHours` | `Int` | | `4` | How long the pass grants access |
| `thumbnailUrl` | `String?` | | `null` | Background image shown behind the paywall panels |

---

### `MatchPassGrant`

Returned in `onAccessGranted`. Present `token` to your streaming backend to authorise playback.

| Field | Type | Description |
|-------|------|-------------|
| `token` | `String` | Pass token — validate server-side before serving the stream |
| `contentId` | `String` | The `id` from the `MatchPassContent` you passed in |
| `expiresAt` | `String` | ISO 8601 expiry timestamp |

---

### `MatchPassSDK`

| Method | Description |
|--------|-------------|
| `init(context, config)` | Initialise the SDK. Call once in `Application.onCreate()`. |
| `Paywall(content, onAccessGranted, onDismiss)` | Composable paywall. Manages its own state and lifecycle. |
| `hasAccess(context, contentId): Boolean` | Suspend function. Returns `true` if a valid pass exists for this content. |

---

## Paywall flow

```
EnteringPhone
     │  User submits phone number
     ▼
AwaitingOtp       ← OTP sent to user's phone (SMS)
     │  User submits 6-digit code
     ▼
Confirming        ← Shows content title, price, duration
     │  User confirms
     ▼
ProcessingPayment ← Payment deducted (2.6s simulated in demo mode)
     │
     ▼
Issuing           ← POST /passes/issue
     │
     ▼
Polling           ← GET /passes/validate (polls until active)
     │
     ▼
AccessGranted     ← onAccessGranted(grant) fires
```

**Resume flow** (existing pass):

```
tryWatch called
     │  stored token found
     ▼
Resuming          ← GET /passes/validate (single call)
     │  valid
     ▼
onAccessGranted fires immediately (no UI shown)
```

---

## Demo / sandbox mode

When `debug = true` and the server is running with `?demo=true` (default), the OTP is returned in the API response and displayed as a gold hint card above the OTP input field. No SMS is required.

For local development against the MatchPass API running on your machine:

```kotlin
MatchPassConfig(
    apiKey  = "your-demo-key",
    baseUrl = "http://10.0.2.2:8002/api/v1/",  // emulator → host localhost
    debug   = true,
)
```

Add this to your `AndroidManifest.xml` to allow cleartext to the emulator host:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

`res/xml/network_security_config.xml`:

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

---

## What the SDK does NOT do

- **Payment processing** — MatchPass issues entitlements after your payment provider confirms success. In the demo the payment step is simulated. In production, your backend calls `/passes/issue` after the payment webhook.
- **Video playback** — the SDK returns a token. Your app passes it to your streaming backend and starts the player.
- **User accounts** — MatchPass identifies users by phone number (OTP-verified). No accounts, no passwords, no PII stored beyond the phone number.

---

## Versioning

| Version | Notes |
|---------|-------|
| 0.1.0 | Initial release — OTP auth, pass issuance, local persistence, Compose paywall |

---

## License

Private — © MatchPass Africa. All rights reserved.  
Contact [hello@matchpass.africa](mailto:hello@matchpass.africa) for licensing and integration support.
