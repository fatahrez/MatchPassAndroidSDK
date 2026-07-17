[![](https://jitpack.io/v/fatahrez/MatchPassAndroidSDK.svg)](https://jitpack.io/#fatahrez/MatchPassAndroidSDK)

# MatchPass Android SDK

Pay-per-view passes for streaming platforms — powered by M-Pesa.

Add one composable. MatchPass handles phone verification, M-Pesa STK Push payment, pass issuing, and access validation. Users pay with one tap. No subscription required.

---

## How it works

```
User taps locked content
        │
        ▼
MatchPassSDK.Paywall()
        │
        ├─ Existing pass? ──────────────────────► onAccessGranted (no UI shown)
        │
        ├─ Phone known? ─── Yes ───────────────► Confirm screen
        │                                              │
        │                    No                        ▼
        │                    │                  M-Pesa STK Push sent
        │                    ▼                         │
        │              Phone entry                     ▼
        │                    │                  User enters PIN on phone
        │                    ▼                         │
        │              OTP verification                ▼
        │                                       Pass issued automatically
        │                                             │
        └─────────────────────────────────────────────▼
                                              onAccessGranted(grant)
```

---

## Requirements

| | Minimum |
|---|---|
| Android | API 24 (Android 7.0) |
| Kotlin | 1.9+ |
| Jetpack Compose BOM | 2024.09.00+ |
| compileSdk | 35 |

---

## Installation

### 1. Add JitPack to `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency

```kotlin
dependencies {
    implementation("com.github.fatahrez:MatchPassAndroidSDK:1.0.0-beta04")
}
```
  
> The version string must match a git tag on the repo.

### Local / evaluation setup

Clone this repo alongside your project and include it as a module:

```kotlin
// settings.gradle.kts
include(":matchpass-sdk")
project(":matchpass-sdk").projectDir = File("../MatchPassAndroidSDK/sdk")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":matchpass-sdk"))
}
```

---

## Setup

Initialise once in `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MatchPassSDK.Builder(this)
            .apiKey("YOUR_API_KEY")       // from the MatchPass operator dashboard
            .debug(BuildConfig.DEBUG)
            .initialize()
    }
}
```

Get your API key at [dashboard.matchpass.africa](https://dashboard.matchpass.africa) — it is generated when you register and shown once.

---

## Show the paywall

```kotlin
// In your composable — show when a user taps locked content
if (showPaywall) {
    MatchPassSDK.Paywall(
        content   = MatchPassContent(
            id            = "arsenalmancity01jul26",
            title         = "Arsenal vs Manchester City",
            price         = "299",
            currency      = "KSh",
            durationHours = 4,
            contentType   = ContentType.MATCH,
        ),
        userPhone = loggedInPhone,      // null → SDK will ask; non-null → skips OTP
        onAccessGranted = { grant ->
            showPaywall = false
            startStream(grant.token)    // present this token to your streaming backend
        },
        onDismiss = { showPaywall = false },
    )
}
```

The paywall is fully self-contained. It handles:
- Phone entry and OTP verification (skipped if `userPhone` is provided)
- M-Pesa STK Push — user receives a prompt on their phone and enters their PIN
- Pass issuing and local storage
- Restoring a previously purchased pass after reinstall or device switch

---

## Check access (no UI)

Use this to decide whether to show a play button or a lock icon on content cards:

```kotlin
LaunchedEffect(content.id) {
    when (val result = MatchPassSDK.checkAccess(context, content)) {
        is AccessResult.Granted      -> showPlayButton(result.grant.token)
        is AccessResult.Expired      -> showRenewButton()
        is AccessResult.NotPurchased -> showLockIcon()
        is AccessResult.Error        -> showLockIcon()  // fail closed on errors
    }
}
```

`checkAccess()` respects the content's cache TTL — for `MOVIE` it trusts local storage for up to 30 days without a server round-trip.

---

## Optional: phone login composable

Show a standalone phone-verification screen before users browse content. Once verified, `Paywall()` calls skip OTP automatically.

```kotlin
MatchPassSDK.Login(
    onLoggedIn = { phone -> sessionPhone = phone },
    onSkip     = { /* user dismissed */ },
)
```

---

## Optional: set phone from your own auth

If your app has its own login system and already has a verified phone, pass it to MatchPass so the paywall skips its OTP step:

```kotlin
// After your own login succeeds
MatchPassSDK.setPhone(context, verifiedPhone)
```

---

## Session management

```kotlin
// Returns the verified phone stored on this device, or null if not logged in
val phone: String? = MatchPassSDK.getStoredPhone(context)

// Clear the session — does NOT revoke passes
MatchPassSDK.signOut(context)

// Epoch-millisecond expiry for a content pass (for scheduling UI re-checks)
val expiresAt: Long? = MatchPassSDK.getExpiresAt(context, contentId)
```

---

## Content types

| `ContentType` | Use case | Cache TTL | Pass label |
|---|---|---|---|
| `MATCH` | Live sport, single event | 5 min | Game Pass |
| `CHANNEL` | Live channel, time-limited | 60 s | Channel Pass |
| `SEASON` | Full TV season, rewatch | 24 h | Season Pass |
| `MOVIE` | Movie, own permanently | 30 days | Own it |

The SDK picks the right validation frequency and UI copy automatically from `ContentType`.

Override for full control:

```kotlin
MatchPassContent(
    id     = "boxing-ppv-05jul26",
    policy = PassPolicy(
        cacheTtlSeconds = 2 * 60L,
        allowRewatch    = false,
        showCountdown   = true,
        passLabel       = "PPV Pass",
    ),
)
```

---

## Content IDs

Use human-readable, date-stamped IDs for live events. This ensures the same fixture in a future season gets a new ID and a fresh pass.

```kotlin
// ✅ Recommended — unique per fixture and date
id = "arsenalmancity01jul26"
id = "chiefspirates03jul26"
id = "boxing-riyadhseason05jul26"

// ✅ Channels and series — stable IDs (content repeats)
id = "ch-supersport1"
id = "series-shaka-ilembe-s1"

// ❌ Avoid — generic IDs cause pass collisions across seasons
id = "match-001"
```

---

## Pass restore

When a user reinstalls or switches devices, the SDK automatically checks the server for an existing active pass before showing the paywall. If found, `onAccessGranted` fires silently — no re-purchase.

This works automatically when the user's phone is known (set via `setPhone()` or after a previous OTP login).

---

## Debug / staging integration

```kotlin
MatchPassSDK.Builder(this)
    .apiKey("your-staging-key")
    .environment(MatchPassEnvironment.STAGING)  // no URL to type — the SDK knows the staging host
    .debug(true)                                // enables OkHttp logging
    .initialize()
```

There's no way to point the SDK at an arbitrary URL — only `STAGING` and `PRODUCTION` (the default) are available, so there's nothing to misconfigure.

When the backend's `demo=true` echo is active, the OTP code is displayed as a hint on screen. Whether an actual SMS/WhatsApp message goes out is controlled server-side by environment, not by this flag — MatchPass's staging and production backends both send for real.

`res/xml/network_security_config.xml`:

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

---

## API reference

### `MatchPassSDK`

| Method | Returns | Description |
|---|---|---|
| `init(context, config)` | `Unit` | Initialise the SDK. Call once from `Application.onCreate()`. |
| `Builder(context)` | `Builder` | Fluent alternative to `init()`. |
| `Paywall(...)` | `Unit` *(Composable)* | Show the purchase flow. |
| `Login(...)` | `Unit` *(Composable)* | Standalone phone OTP login screen. |
| `checkAccess(context, content)` | `AccessResult` | Validate access without UI. Suspend function. |
| `setPhone(context, phone)` | `Unit` | Pre-set a verified phone (from your own auth). |
| `getStoredPhone(context)` | `String?` | Read the verified phone stored on device. |
| `signOut(context)` | `Unit` | Clear the session. |
| `getExpiresAt(context, contentId)` | `Long?` | Epoch-ms expiry for a content pass. |

### `AccessResult`

| Subtype | Fields | Meaning |
|---|---|---|
| `Granted` | `grant: MatchPassGrant` | Valid pass — start the stream |
| `Expired` | `expiredAt: String` | Pass found but expired — show renewal |
| `NotPurchased` | — | No pass — show paywall |
| `Error` | `exception: MatchPassException` | Network/server error — fail closed |

### `MatchPassGrant`

| Field | Type | Description |
|---|---|---|
| `token` | `String` | Present to your streaming backend to authorise playback |
| `contentId` | `String` | Matches the `id` you passed in `MatchPassContent` |
| `expiresAt` | `String` | ISO 8601 expiry timestamp |

---

## Publishing (JitPack)

Tag a release and push — JitPack builds on first request:

```bash
git tag 1.0.0-beta01
git push origin 1.0.0-beta01
```

Then open `https://jitpack.io/#YOUR_USERNAME/MatchPassAndroidSDK` and click **Get it** to pre-build.

### Private distribution options

| Option | Cost | How |
|---|---|---|
| JitPack Pro | ~$14/month | Private repo supported on paid plan |
| GitHub Packages | Free | Publish to `maven.pkg.github.com` — operators use a read-only token |
| Direct AAR | Free | Attach `.aar` to a GitHub Release; operators add as a local dep |

---

## Running tests

```bash
./gradlew :sdk:test
```

Covers: `PassPolicy`, `AccessChecker`, `PaywallViewModel` state machine, `MatchPassStore`.

---

## License

Copyright © 2026 MatchPass Africa. All rights reserved.  
Contact [globalhcsolution@gmail.com](mailto:globalhcsolution@gmail.com) for licensing and operator onboarding.
