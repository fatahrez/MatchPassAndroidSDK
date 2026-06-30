# MatchPass Android SDK

Pay-per-view access infrastructure for Android apps.  
Drop in one composable. MatchPass handles OTP auth, pass issuance, entitlement validation, and local persistence — your app just receives a token and starts the stream.

---

## How it works

```
Your app                      MatchPass SDK                    MatchPass API
────────                      ─────────────                    ─────────────
User taps Watch ──────────▶  checkAccess(content)
                              ├─ cached, fresh ──────────────▶  (skipped)
                              └─ stale / no pass ────────────▶  /passes/validate

No pass found or expired:
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

The entire purchase flow is contained in a single composable: `MatchPassSDK.Paywall(...)`.  
On subsequent opens, the stored pass is validated according to the content's [PassPolicy](#passpolicy) — live content is re-validated frequently, owned content is trusted from cache.

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
    implementation("com.github.MatchPassAfrica:MatchPassAndroidSDK:1.0.0")
}
```

### Local (development / evaluation)

Clone this repo alongside your project, then add it as a module in `settings.gradle.kts`:

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

        // Option A — Builder (recommended, discoverable)
        MatchPassSDK.Builder(this)
            .apiKey("your-operator-api-key")      // from the MatchPass dashboard
            .baseUrl("https://api.matchpass.africa/api/v1/")
            .debug(BuildConfig.DEBUG)
            .initialize()

        // Option B — direct init
        MatchPassSDK.init(
            context = this,
            config  = MatchPassConfig(apiKey = "your-operator-api-key"),
        )
    }
}
```

`init()` and `Builder.initialize()` are idempotent — safe to call more than once.

---

## Content types

Every piece of content has a `ContentType` that tells the SDK how to behave.
Set it once on `MatchPassContent` — the SDK handles validation frequency, UI copy, and caching automatically.

| Type | Business model | Pass label | Re-validation | Rewatch |
|------|---------------|-----------|--------------|---------|
| `MATCH` | One game, short window | "Game Pass" | Every 5 min | No |
| `CHANNEL` | Live channel access | "Channel Pass" | Every 60 s | No |
| `SEASON` | Season ownership | "Season Pass" | Once per 24 h | Yes |
| `MOVIE` | Perpetual ownership | "Own it" | Once per 30 days | Yes |

```kotlin
// Game pass — expires after match window
MatchPassContent(
    id           = "epl-match-001",
    title        = "Arsenal vs Manchester City",
    price        = "29.00",
    contentType  = ContentType.MATCH,
)

// Live channel — tight re-validation
MatchPassContent(
    id           = "supersport-1",
    title        = "SuperSport 1",
    price        = "15.00",
    durationHours = 4,
    contentType  = ContentType.CHANNEL,
)

// Season ownership — rewatch freely
MatchPassContent(
    id           = "shaka-ilembe-s1",
    title        = "Shaka Ilembe Season 1",
    price        = "59.00",
    durationHours = 720,                // 30 days
    contentType  = ContentType.SEASON,
)

// Movie — own it forever
MatchPassContent(
    id           = "movie-001",
    title        = "The Woman King",
    price        = "35.00",
    contentType  = ContentType.MOVIE,
)
```

### Custom policy

Override cache TTL or any other policy parameter per content:

```kotlin
MatchPassContent(
    id          = "boxing-event",
    title       = "Riyadh Season Boxing",
    price       = "49.00",
    contentType = ContentType.MATCH,
    policy      = PassPolicy.MATCH.copy(cacheTtlSeconds = 10 * 60L),  // 10-min cache
)
```

---

## Show the paywall

```kotlin
@Composable
fun ContentScreen(content: MatchPassContent, onStartStream: (token: String) -> Unit) {
    var showPaywall by remember { mutableStateOf(false) }

    if (showPaywall) {
        MatchPassSDK.Paywall(
            content         = content,
            onAccessGranted = { grant ->
                showPaywall = false
                onStartStream(grant.token)  // pass token to your streaming backend
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
- **Returning user** — validates the stored pass (cache-aware per `ContentType`). If still valid, `onAccessGranted` fires immediately with no UI shown.
- **Expired pass** — clears local storage, shows an error, restarts the purchase flow.

---

## Check access without UI

Use `checkAccess()` to gate UI elements or decide whether to show a "Resume" button:

```kotlin
LaunchedEffect(content.id) {
    when (val result = MatchPassSDK.checkAccess(context, content)) {
        is AccessResult.Granted      -> showResumeButton(result.grant)
        is AccessResult.Expired      -> showRenewalBadge()
        is AccessResult.NotPurchased -> showWatchButton()
        is AccessResult.Error        -> showWatchButton()  // fail open on network errors
    }
}
```

`checkAccess()` respects the `PassPolicy` cache TTL — for `MOVIE` content this returns
`Granted` from local cache for up to 30 days without a server round-trip.

---

## Lock content (gating)

Show a lock indicator on content the user hasn't purchased:

```kotlin
@Composable
fun ContentCard(content: MatchPassContent) {
    var isLocked by remember { mutableStateOf(true) }

    LaunchedEffect(content.id) {
        isLocked = MatchPassSDK.checkAccess(context, content) !is AccessResult.Granted
    }

    Box {
        Thumbnail(content.thumbnailUrl)
        if (isLocked) LockBadge(price = "${content.currency} ${content.price}")
    }
}
```

---

## API reference

### `MatchPassSDK.Builder`

| Method | Description |
|--------|-------------|
| `apiKey(String)` | Required. Your operator API key. |
| `baseUrl(String)` | Optional. Override for staging. Default: `https://api.matchpass.africa/api/v1/`. |
| `debug(Boolean)` | Optional. Enables OkHttp request/response logging. |
| `initialize()` | Applies configuration and initialises the SDK. |

---

### `MatchPassConfig`

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `apiKey` | `String` | ✓ | — | Your operator API key from the MatchPass dashboard |
| `baseUrl` | `String` | | `https://api.matchpass.africa/api/v1/` | Override for staging or self-hosted |
| `debug` | `Boolean` | | `false` | Enables full OkHttp request/response logging |

---

### `MatchPassContent`

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `id` | `String` | ✓ | — | Must match the `external_id` registered in the MatchPass dashboard. Mismatch → HTTP 400. |
| `title` | `String` | ✓ | — | Displayed on the paywall confirm screen |
| `price` | `String` | ✓ | — | Amount as decimal string, e.g. `"29.00"` |
| `currency` | `String` | | `"ZAR"` | ISO 4217 code |
| `durationHours` | `Int` | | `4` | How long the pass grants access (shown on confirm screen) |
| `thumbnailUrl` | `String?` | | `null` | Background image shown behind the paywall panels |
| `contentType` | `ContentType` | | `MATCH` | Semantic type — SDK derives `policy` automatically |
| `policy` | `PassPolicy` | | `PassPolicy.forType(contentType)` | Override to customise cache TTL or UI copy |

---

### `ContentType`

| Value | Description |
|-------|-------------|
| `MATCH` | Single live event — game pass, short window |
| `CHANNEL` | Live broadcast channel — time-limited access |
| `SEASON` | TV season ownership — rewatch freely |
| `MOVIE` | Movie — own it permanently |

---

### `PassPolicy`

| Field | Type | Description |
|-------|------|-------------|
| `cacheTtlSeconds` | `Long` | How long the SDK trusts a cached validation before re-checking with the server |
| `allowRewatch` | `Boolean` | Whether the user can return to the content after the first session |
| `showCountdown` | `Boolean` | Whether to show "X hours remaining" on the access-granted screen |
| `passLabel` | `String` | Label shown on the confirm screen, e.g. "Game Pass", "Own it" |

Presets: `PassPolicy.MATCH`, `PassPolicy.CHANNEL`, `PassPolicy.SEASON`, `PassPolicy.MOVIE`.

---

### `AccessResult`

| Subtype | Fields | When returned |
|---------|--------|---------------|
| `Granted` | `grant: MatchPassGrant` | Valid pass found (local cache or server) |
| `Expired` | `expiredAt: String` | Pass found but past expiry |
| `NotPurchased` | — | No pass stored |
| `Error` | `exception: MatchPassException` | Network or server error |

On `Error`, the stored pass is **not** cleared — the SDK fails open so a transient network issue doesn't lock out a paying user.

---

### `MatchPassGrant`

| Field | Type | Description |
|-------|------|-------------|
| `token` | `String` | Present this to your streaming backend to authorise playback |
| `contentId` | `String` | The `id` from the `MatchPassContent` you passed in |
| `expiresAt` | `String` | ISO 8601 expiry timestamp |

---

### `MatchPassException`

All SDK errors are subtypes of `MatchPassException`:

| Subtype | Meaning |
|---------|---------|
| `NotInitialized` | `init()` was not called before use |
| `ContentNotFound` | `content.id` doesn't match any registered content |
| `InvalidOtp` | OTP code was wrong |
| `OtpExpired` | OTP expired before the user submitted it |
| `PassExpired` | The pass exists but is past its expiry date |
| `PassRevoked` | The pass was revoked by the operator |
| `NetworkError` | An `IOException` occurred |
| `ServerError` | Unexpected HTTP error from the API |
| `ConfigurationError` | SDK misconfiguration (missing API key, etc.) |

---

## Paywall flow

```
EnteringPhone
     │  User submits phone number
     ▼
AwaitingOtp       ← OTP sent to user's phone (SMS)
     │  User submits 6-digit code
     ▼
Confirming        ← Shows content title, price, duration, pass type
     │  User confirms
     ▼
ProcessingPayment ← Payment deducted
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

**Returning user** (existing valid pass):

```
Paywall composable mounted
     │  stored token found
     ▼
Resuming          ← AccessChecker.check() (cache or server)
     │  valid
     ▼
onAccessGranted fires immediately (no UI shown)
```

---

## Demo / sandbox mode

When `debug = true` and the server is running in demo mode, the OTP is returned in the API response and displayed as a gold hint card above the OTP input. No real SMS is sent.

For local development against the MatchPass API on your machine:

```kotlin
MatchPassSDK.Builder(this)
    .apiKey("your-demo-operator-key")
    .baseUrl("http://10.0.2.2:8002/api/v1/")  // emulator → host localhost
    .debug(true)
    .initialize()
```

Add cleartext permission to `AndroidManifest.xml`:

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

## Testing your integration

Run the SDK's own test suite:

```bash
./gradlew :sdk:test
```

Tests cover:
- `PassPolicyTest` — content type → policy mapping, cache TTL ordering
- `AccessCheckerTest` — cache hit/miss logic, server validation, error handling per content type
- `PaywallViewModelTest` — full state machine (EnteringPhone → AccessGranted)
- `MatchPassStoreTest` — pass persistence and validation timestamp isolation

---

## What the SDK does NOT do

- **Payment processing** — MatchPass issues entitlements. In the demo, the payment step is simulated. In production, your payment provider calls `/passes/issue` after a successful charge webhook.
- **Video playback** — the SDK returns a token. Your app passes it to your streaming backend and starts the player.
- **User accounts** — MatchPass identifies users by OTP-verified phone number only. No accounts, no passwords.

---

## Versioning

| Version | Notes |
|---------|-------|
| 1.0.0 | Content type system (`ContentType`, `PassPolicy`), typed errors (`MatchPassException`), `AccessResult`, `Builder`, `checkAccess()`, unit test suite |
| 0.1.0 | Initial release — OTP auth, pass issuance, local persistence, Compose paywall |

---

## License

Private — © MatchPass Africa. All rights reserved.  
Contact [hello@matchpass.africa](mailto:hello@matchpass.africa) for licensing and integration support.
