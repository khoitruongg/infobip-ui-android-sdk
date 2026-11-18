# infobip-ui-sdk

Android UI SDK wrapping **Infobip WebRTC calling** with **Jetpack Compose** call screens and components.

This README is for **client/host apps** integrating the SDK. See [CHANGELOG.md](CHANGELOG.md) for release history.

## Requirements

- **AGP 8.x** (this repository uses AGP 8.13.2 with Gradle 8.13)
- Android app using **Jetpack Compose**, with `minSdk 23` and `compileSdk 35` or newer
- JDK 17 for the Gradle build; SDK bytecode targets Java 11
- Firebase Cloud Messaging (FCM) for incoming call push notifications
- A backend that issues **Infobip WebRTC tokens** for your users (do **not** ship Infobip API keys in your app)

## Install (Gradle)

Add the dependency to your app module.

> If you consume this SDK as a published artifact, use the coordinates provided by your Infobip contact / your internal Maven repository.
> If you consume it from source, include this repository as a Gradle composite build or module dependency and use a Gradle version compatible with your AGP 8.x release.

The SDK supports host apps using **AGP 8.x** without additional AGP-specific configuration.

If a host app uses **AGP 9+**, add the following compatibility setting to its `gradle.properties` because Infobip RTC dependencies currently share a namespace:

```properties
android.uniquePackageNames=false
```

## Host app setup

### Firebase (required for incoming call push)

- Add `google-services.json` to your app module (e.g. `app/google-services.json`)
- Apply the Google Services plugin in your app build

### Android Manifest

The SDK merges its own manifest entries (permissions, `IncomingCallActionReceiver`). Your app must declare its own `FirebaseMessagingService` and forward call push to the SDK as described below.

#### Incoming call Activity

Set your incoming-call entry Activity to `singleTop` so it can receive notification intents reliably. Optionally add `android:showWhenLocked="true"` and `android:turnScreenOn="true"` if you want the screen to wake for incoming calls.

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    android:showWhenLocked="true"
    android:turnScreenOn="true">
    <!-- your launcher intent-filter, etc. -->
</activity>
```

Forward call-related intents to the SDK using `InfobipCallIntents` (see [Handle incoming call intents](#handle-incoming-call-intents-in-your-activity) below).

#### Firebase Cloud Messaging (FCM)

The SDK does **not** register a `FirebaseMessagingService`. Your app must own the single `MESSAGING_EVENT` service and forward Infobip call push to the SDK via `InfobipUiSdkFcmDelegate`.

```xml
<service
    android:name=".YourFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

```kotlin
class YourFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Your app-specific handling (analytics, other providers, etc.)
        InfobipUiSdkFcmDelegate.onMessageReceived(this, remoteMessage)
    }

    override fun onNewToken(token: String) {
        // Your app-specific handling (send token to your backend, etc.)
        InfobipUiSdkFcmDelegate.onNewToken(this, token)
    }
}
```

`InfobipUiSdkFcmDelegate` only processes Infobip incoming-call payloads; other FCM messages are ignored by the SDK and remain yours to handle.

> See `example/src/main/AndroidManifest.xml` and `example/.../ExampleFirebaseMessagingService.kt` for a full working reference.

### Permissions

Your app must request and be granted:

- `RECORD_AUDIO`
- `CAMERA` (if you enable video)
- `POST_NOTIFICATIONS` (Android 13+, for call notifications)
- `VIBRATE` (incoming-call vibration; merged from the SDK manifest)
- `BLUETOOTH_CONNECT` (Android 12+, when using BT headsets)

You can use `InfobipCallPermissions.requestCallPermissions(activity)` or request the same permissions yourself.

`MicButton` (and `CallControlBar`’s mic control) also handles missing mic access at runtime: if `RECORD_AUDIO` is not granted, the control shows as muted and stays tappable. The first tap requests the permission; if the user has permanently denied it, the SDK shows a dialog (copy from `CallStrings.micPermission*`) that opens app settings. After the user returns from Settings, the control refreshes automatically.

## Initialize the SDK

Initialize once (typically in `Application.onCreate()`), provide:

- `pushConfigId` from Infobip
- `incomingCallActivityClass` (the Activity that will open for incoming calls)
- Optional `tokenProvider` that fetches a fresh **WebRTC token from your backend** when the cached token expires. If you omit it, the SDK keeps the token from `registerSubscriber` / `updateToken` for outgoing and incoming calls (it will not clear that cache on retry paths).

```kotlin
val callClient = InfobipCallSdk.initialize(
    application = this,
    config = InfobipCallConfig(
        pushConfigId = BuildConfig.PUSH_CONFIG_ID,
        incomingCallActivityClass = MainActivity::class.java,
        // Optional — omit to rely on the token passed to registerSubscriber / updateToken
        tokenProvider = { identity -> backend.getWebRtcToken(identity) },
        // Optional — custom IncomingCall ringtone (default = system ringtone)
        // incomingCallRingtoneUri = Uri.parse("android.resource://$packageName/${R.raw.my_ringtone}"),
        // Optional — custom outgoing waiting/ringback tone (default = SDK R.raw.outgoing_waiting_ringtone)
        // outgoingCallWaitingRingtoneUri = Uri.parse("android.resource://$packageName/${R.raw.my_ringback}"),
        // Optional — show incoming-call notification while app is in foreground (default = true)
        // showIncomingCallNotificationInForeground = false,
    ),
)
```

## Register a user (subscriber) and enable incoming calls

When the user signs in (or whenever you have a valid WebRTC token):

```kotlin
callClient.registerSubscriber(
    identity = userId,
    displayName = userName,
    token = webrtcToken,
)

try {
    callClient.registerForIncomingCalls()
    // Or override the push config for this registration:
    // callClient.registerForIncomingCalls(pushConfigId = "your-push-config-id")
} catch (e: Exception) {
    // Registration failures are propagated (not swallowed). Retry after fixing token / network / pushConfigId.
}

// Blank pushConfigId skips FCM registration (foreground WebSocket only); background/killed
// incoming calls require a non-blank Infobip pushConfigId.
```

On logout (including force logout on this device), clear the subscriber so the active Infobip RTC connection is disconnected, push registration is disabled, and this device ignores stale IncomingCall FCM until re-registered:

```kotlin
callClient.clearSubscriber()
```

After logout, call `registerSubscriber` and `registerForIncomingCalls()` again before expecting incoming calls. Re-registration reconnects the existing Infobip RTC active connection (Infobip’s singleton becomes `ActiveConnectionInfobipRTC` after the first register; a plain re-register alone would otherwise be a no-op).

## Show the Compose call UI

Wrap your call UI with `InfobipCallSdk.Theme` and optionally pass:

- `appearance: CallAppearance` — colors, fonts, metrics, icons, gradient
- `strings: CallStrings` — every user-facing string (English defaults)

```kotlin
InfobipCallSdk.Theme(
    appearance = CallAppearance(
        colors = CallColors(accent = myBrandColor, accept = myGreen, decline = myRed),
        fonts = CallFonts(
            name = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            status = TextStyle(fontSize = 15.sp, color = mySecondaryText),
            button = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        ),
        icons = CallIcons(
            accept = Icons.Filled.Phone,
            hangUp = Icons.Filled.CallEnd,
            mute = Icons.Filled.Mic,
            unmute = Icons.Filled.MicOff,
            speaker = Icons.AutoMirrored.Filled.VolumeUp,
        ),
    ),
    strings = CallStrings(
        freeCallTitle = "Cuộc gọi miễn phí",
        connecting = "Đang kết nối…",
    ),
) {
    // IncomingCallScreen / OutgoingCallScreen / custom UI
}
```

Legacy hosts can still pass only `CallUiTheme(colors = …)` via the back-compat `Theme(theme = …)` overload.

See `example/.../ExampleCallTheme.kt` for a runnable branded sample wired from `MainActivity`.

The SDK provides Compose screens you can place into your navigation:

- `IncomingCallScreen(client, onCallEnded, …)`
- `OutgoingCallScreen(client, destinationIdentity, destinationDisplayName, customData, onCallEnded, …)`
- `UnreachableCallScreen`
- `AudioRouteSheet`
- `KeypadScreen`

It also provides building-block components such as `CallAvatar`, `CallControlBar`, `MicButton`, `SpeakerButton`, `AcceptCallButton`, `DeclineCallButton`, `HangUpCallButton`, `MessageButton`, `CallStatus`, and `CallSignalStrength`.

These components read defaults from `CallAppearance` / `CallStrings`. For custom screens, you can also override **icon**, **color**, and **text style** per call:

```kotlin
AcceptCallButton(
    client = callClient,
    icon = Icons.Filled.Phone,
    backgroundColor = myGreen,
    iconTint = Color.White,
)

MicButton(
    client = callClient,
    icon = Icons.Filled.Mic,
    activeIcon = Icons.Filled.MicOff,
    activeBackgroundColor = myMuteRed,
    label = "Mute",
    activeLabel = "Unmute",
    labelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    onPress = { /* optional: analytics before mute toggle / permission request */ },
)

CallStatus(
    client = callClient,
    titleStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    statusStyle = TextStyle(fontSize = 14.sp),
    titleColor = Color.White,
    activeStatusColor = myGreen,
)

CallAvatar(
    participant = remote,
    placeholderIcon = Icons.Filled.Person,
    placeholderBackground = myTealLight,
    initialsStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
)

// Network quality (Infobip RTC). Not shown on default screens — place in custom UIs.
//
// Option A — CallSignalStrength: default strong/weak copy from CallStrings
// (override per call via strongText / weakText, or theme-wide via CallStrings).
CallSignalStrength(client = callClient)
CallSignalStrength(
    quality = session?.localNetworkQuality,
    strongText = "Good connection",
    weakText = "Poor connection",
)

// Option B — CallNetworkQuality: map each enum to your own text in a custom screen.
when (session?.localNetworkQuality) {
    CallNetworkQuality.Excellent -> "Excellent connection"
    CallNetworkQuality.Good -> "Good connection"
    CallNetworkQuality.Fair -> "Fair connection"
    CallNetworkQuality.Poor -> "Poor connection"
    CallNetworkQuality.Bad -> "Bad connection"
    null -> null // still unknown — hide or show a placeholder
}

CallControlBar(
    client = callClient,
    showSpeaker = true,
    showMic = true,
    showMessage = true,
    onMessageClick = { /* host handles chat */ },
    onMicPress = { /* optional */ },
    onSpeakerPress = { /* optional */ },
    labelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    backgroundColor = myControlBg,
    activeBackgroundColor = myBrandColor,
    micStyle = CallControlStyle(
        icon = Icons.Filled.Mic,
        activeIcon = Icons.Filled.MicOff,
        activeBackgroundColor = myMuteRed,
        label = "Mute",
        activeLabel = "Unmute",
    ),
    speakerStyle = CallControlStyle(
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        label = "Speaker",
    ),
    messageStyle = CallControlStyle(label = "Message"),
)
```

Unset parameters keep the current `CallAppearance` values. For fully custom layouts, use the slot-based `CallControlBar { … }` and pass overrides on each child button.
### Outgoing call identity & metadata

Pass a display name for the callee, and optional `customData` that is forwarded to the remote side (e.g. so the callee UI can show your name/avatar):

```kotlin
OutgoingCallScreen(
    client = callClient,
    destinationIdentity = "alice",
    destinationDisplayName = "Alice",
    customData = mapOf(
        "userName" to currentUserName,
        "avatarUrl" to currentUserAvatarUrl,
    ),
    onCallEnded = { /* pop back */ },
)
```

### Call end codes

Terminal sessions expose Infobip hangup status so hosts can branch on specific outcomes:

```kotlin
val session by callClient.activeSession.collectAsState(initial = null)
when (session?.endCode?.id) {
    CallEndCode.ID_BUSY -> { /* callee busy */ }
    CallEndCode.ID_NO_ANSWER -> { /* no answer */ }
    CallEndCode.ID_CANCELLED -> { /* cancelled before connect */ }
    CallEndCode.ID_REJECTED -> { /* declined / rejected */ }
    CallEndCode.ID_NORMAL_HANGUP -> when (session?.endedBy) {
        CallEndedBy.Local -> { /* this device hung up */ }
        CallEndedBy.Remote -> { /* remote peer hung up */ }
        null -> Unit
    }
}
```

- `CallSession.endCode` – Infobip status `id` / `name` / `description` (same ids as Portunus `status`)
- `CallSession.endedBy` – `Local` (this device) or `Remote` (peer / platform)
- `CallSession.state` – `Ended` for normal completion codes, `Failed` for busy / cancel / reject / errors
- `CallSession.failureReason` – display text derived from the status description

### Call duration

Use `callClient.callDurationSeconds()` (from Infobip RTC `WebrtcCall.duration()`). Default screens and `CallStatus` already use this. Custom UIs can poll it at any time — it returns `0` and does not throw when the call is not established or is tearing down:

```kotlin
var durationSeconds by remember { mutableIntStateOf(client.callDurationSeconds()) }
LaunchedEffect(Unit) {
    while (true) {
        durationSeconds = client.callDurationSeconds()
        delay(1_000)
    }
}
```

Show the formatted value when `CallState.Active`; other states typically use status text instead.

### Custom call screens (no SDK default screens)

If you don’t want the default screens, build your own by:

- observing `callClient.activeSession`
- calling `acceptIncomingCall()`, `declineIncomingCall()`, `hangUp()`, `setMuted(...)`, `setSpeakerOn(...)`, `selectAudioRoute(...)`, `sendDtmf(...)`, `retryLastCall()`
- composing the building-block UI components

The `example` module includes reference implementations:

- `example/.../ExampleCallTheme.kt` — sample `CallAppearance` / `CallStrings` branding applied via `InfobipCallSdk.Theme`
- `example/.../customui/CustomIncomingCallScreen.kt`
- `example/.../customui/CustomOutgoingCallScreen.kt`

Minimal pattern used by the example app:

```kotlin
val session by callClient.activeSession.collectAsState(initial = null)
val callState = session?.state
val showIncoming = callState == CallState.Incoming || callState == CallState.Active

when {
    outgoingTo != null -> CustomOutgoingCallScreen(
        client = callClient,
        destinationIdentity = outgoingTo.orEmpty(),
        destinationDisplayName = outgoingDisplayName,
        customData = mapOf("userName" to userName),
        onCallEnded = { outgoingTo = null },
        onCallFailed = { outgoingTo = null },
    )
    showIncoming -> CustomIncomingCallScreen(
        client = callClient,
        onCallEnded = { /* return to home UI */ },
    )
    else -> { /* your app UI */ }
}
```

## Handle incoming call intents in your Activity

Your `MainActivity` (or the Activity specified as `incomingCallActivityClass`) should be `singleTop` and handle new intents:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingCallIntent(intent)
}
```

Minimal intent handling (based on `InfobipCallIntents` constants):

```kotlin
private fun handleIncomingCallIntent(intent: Intent) {
    when (intent.action) {
        InfobipCallIntents.ACTION_ACCEPT -> {
            InfobipCallPush.dismissIncomingCallNotification(this)
            lifecycleScope.launch {
                callClient.acceptIncomingCall()
            }
            intent.action = null
        }
        InfobipCallIntents.ACTION_DECLINE -> {
            InfobipCallPush.dismissIncomingCallNotification(this)
            lifecycleScope.launch {
                callClient.declineIncomingCall()
            }
            intent.action = null
        }
    }

    if (
        intent.getBooleanExtra(InfobipCallIntents.EXTRA_INCOMING_CALL, false) ||
        intent.getBooleanExtra(InfobipCallIntents.EXTRA_OPEN_INCOMING_CALL, false)
    ) {
        InfobipCallPush.cancelIncomingCallNotification(this)
    }
}
```

## Integration notes / common pitfalls

- **Tokens**: must come from your backend; the SDK does not embed Infobip API keys. `tokenProvider` is optional; without it, keep calling `registerSubscriber` / `updateToken` with a valid token before placing or receiving calls.
- **AGP compatibility**: AGP 8.x is supported directly. On AGP 9+, set `android.uniquePackageNames=false` in `gradle.properties` because Infobip RTC dependencies currently share a namespace.
- **FCM**: the SDK does not register a `FirebaseMessagingService`; forward call push and token refreshes from your service via `InfobipUiSdkFcmDelegate`.
- **Logout**: always call `clearSubscriber()` on logout / force logout so the RTC connection is disconnected, push is disabled, and the persisted incoming-calls gate is cleared (stale FCM on this device is ignored). Then `registerSubscriber` + `registerForIncomingCalls()` again; the SDK reconnects the existing active connection after unregister.
- **registerForIncomingCalls failures**: exceptions are propagated to the host — do not assume success without handling errors. A blank `pushConfigId` skips FCM (foreground only).
- **Microphone**: host apps should still request `RECORD_AUDIO` up front; `MicButton` can also request it on tap and route permanently denied users to Settings.
- **Notifications**: on Android 13+, ensure you request `POST_NOTIFICATIONS` before expecting incoming call notifications (background IncomingCall UI depends on the notification / full-screen intent).
- **Ringtone / vibration**: IncomingCall vibrates and plays [InfobipCallConfig.incomingCallRingtoneUri] (or the system default). Outgoing waiting plays [InfobipCallConfig.outgoingCallWaitingRingtoneUri] (or the SDK default `R.raw.outgoing_waiting_ringtone`). Keep `VIBRATE` (merged from the SDK) and do not strip it with manifest mergers.

## Public API (overview)

| API | Role |
|-----|------|
| `InfobipCallSdk` | Initialize + `Theme(appearance, strings)` / legacy `Theme(theme)` |
| `InfobipCallClient` | Call operations + `activeSession` / `callDurationSeconds()` / `audioRoutes` / `selectAudioRoute` / `sendDtmf` / `retryLastCall` |
| `InfobipCallConfig` / `InfobipTokenProvider` | Configuration |
| Appearance | `CallAppearance`, `CallColors`, `CallFonts`, `CallMetrics`, `CallIcons`, `CallStrings`, `CallUiTheme` |
| `CallSession` / `CallState` / `CallEndCode` / `CallEndedBy` / `CallNetworkQuality` / `CallParticipant` / `AudioRoute` | Call models |
| Screens | `IncomingCallScreen`, `OutgoingCallScreen`, `UnreachableCallScreen`, `AudioRouteSheet`, `KeypadScreen` |
| Components | `CallStatus`, `CallSignalStrength`, `CallAvatar`, `CallControlBar` / `CallControlStyle`, action / mic / speaker buttons (theme + per-call icon/color/text overrides; mic requests `RECORD_AUDIO` when needed) |
| Push / intents | `InfobipCallPush`, `InfobipCallIntents`, `InfobipUiSdkFcmDelegate` |
| Permissions | `InfobipCallPermissions` |

## Example app (this repo)

The `example` module is a runnable app that demonstrates end-to-end integration:

- SDK initialization in `ExampleApplication`
- Subscriber registration (errors from `registerForIncomingCalls` surface in the UI) and `clearSubscriber()` on unregister (disconnects RTC, disables push, clears incoming-calls gate)
- Custom incoming/outgoing screens built from SDK components
- Notification intent handling in `MainActivity`
- Host-owned FCM service forwarding to `InfobipUiSdkFcmDelegate`

### Run the example

1. Copy `.env.example` to `.env` and fill in your Infobip credentials (demo-only):

```properties
API_KEY=your_infobip_api_key
API_BASE_URL=api.infobip.com
PUSH_CONFIG_ID=your_infobip_push_config_id
```

2. Add your Firebase `google-services.json` to `example/`.

3. Build and run:

```bash
./gradlew :example:assembleDebug
```

> **Note:** The example fetches tokens directly from Infobip using an API key for demo purposes. In production, tokens must be issued by your own backend.

## Key example files

| File | Purpose |
|------|---------|
| `example/.../ExampleApplication.kt` | SDK initialization + token provider |
| `example/.../MainActivity.kt` | Permissions, intents, register/call UI + `InfobipCallSdk.Theme` |
| `example/.../ExampleCallTheme.kt` | Sample `CallAppearance` / `CallStrings` branding |
| `example/.../ExampleFirebaseMessagingService.kt` | `InfobipUiSdkFcmDelegate` forwarding |
| `example/.../InfobipTokenClient.kt` | Token generation (demo only) |
| `example/.../customui/CustomIncomingCallScreen.kt` | Custom incoming UI reference |
| `example/.../customui/CustomOutgoingCallScreen.kt` | Custom outgoing UI reference |
| `example/src/main/AndroidManifest.xml` | Activity + FCM service declaration |
