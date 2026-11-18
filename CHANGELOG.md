# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `InfobipCallConfig.incomingCallRingtoneUri` to customize the IncomingCall ringtone (falls back to the system default)
- `InfobipCallConfig.outgoingCallWaitingRingtoneUri` to customize the outgoing waiting/ringback tone (falls back to SDK `R.raw.outgoing_waiting_ringtone`)
- SDK default outgoing waiting ringtone asset (`R.raw.outgoing_waiting_ringtone`)
- `destinationDisplayName` on outgoing calls so the callee name is shown on the outgoing call UI
- `InfobipCallClient.callDurationSeconds()` for live call duration from Infobip RTC
- `CallSession` timestamps from Infobip RTC (`startedAt`, `startTime`, `endTime`)
- Host-configurable call UI: `CallAppearance`, `CallColors`, `CallFonts`, `CallMetrics`, `CallIcons`, `CallStrings`, and `InfobipCallSdk.Theme(appearance, strings)` (legacy `Theme(theme)` overload retained)
- Per-component overrides on building blocks (`CallAvatar`, `CallStatus`, `CallControlBar`, `AcceptCallButton`, `DeclineCallButton`, `HangUpCallButton`, `MicButton`, `SpeakerButton`, `MessageButton`): optional icon, color, and text-style parameters fall back to `CallAppearance`
- `CallControlStyle` for per-control styling on the convenience `CallControlBar(client, …)` overload
- Optional `onPress` on `MicButton` / `SpeakerButton`, and `onMicPress` / `onSpeakerPress` on `CallControlBar`, for host analytics or side effects before mute/speaker toggles
- `CallStrings.micPermissionTitle` / `micPermissionMessage` / `micPermissionConfirm` / `micPermissionDismiss` for the mic settings dialog copy
- Audio routing APIs: `AudioRoute`, `audioRoutes()`, `activeAudioRoute()`, `selectAudioRoute(id)`; `CallSession.audioRoutes` / `activeAudioRouteId`
- `InfobipCallClient.retryLastCall()` and `sendDtmf(digit)` (preserves destination identity, custom data, and display name on retry)
- Compose screens: `UnreachableCallScreen`, `AudioRouteSheet`, `KeypadScreen`
- `CallBackground` is a public building-block for custom call screens
- `CallEndCode` / `CallEndedBy` on `CallSession` so hosts can identify Infobip hangup outcomes (busy, cancel, reject, local vs remote hangup)
- `CallNetworkQuality` + `CallSession.localNetworkQuality` / `remoteNetworkQuality` from Infobip RTC network-quality events (map the enum for custom per-level text, or use `CallSignalStrength` for default strong/weak copy)
- `CallSignalStrength` text building-block for custom call screens (strong / weak)
- `CallColors.signalStrong` / `signalWeak` and `CallStrings.strongSignal` / `weakSignal`
- Active calls seed `localNetworkQuality` / `remoteNetworkQuality` to `Excellent` (Infobip only emits events on quality *changes* from that baseline)

### Fixed

- `callDurationSeconds()` no longer crashes when Infobip RTC has torn down the underlying `ApplicationCall` during hangup (host custom UIs can keep polling safely)
- Call timing helpers (`startTime` / `establishTime` / `endTime`) tolerate the same teardown race
- Incoming-call ringtone no longer aborts call delivery when Do Not Disturb blocks `STREAM_RING` volume changes (`SecurityException`)
- Outgoing calls no longer clear the token from `registerSubscriber()` when `tokenProvider` is unset (previously failed with "No cached token")
- Outgoing calls no longer drop the cached token when `registerForIncomingCalls()` has not run yet (`registeredIdentity == null`)
- Incoming call ringtone now plays the system default ringtone (`MediaPlayer` previously had no data source, so sound never started)
- Incoming call vibration is started with the ringtone (and the notification channel enables vibration)
- Outgoing waiting ringtone now plays the SDK `R.raw.outgoing_waiting_ringtone` asset (previously `MediaPlayer` had no data source / silent ToneGenerator fallback; optionally overridden via config)
- Outgoing waiting ringtone starts from the call repository (so custom outgoing screens such as the example `CustomOutgoingCallScreen` hear ringback, not only `OutgoingCallScreen`)
- `registerForIncomingCalls()` failures are propagated to the host instead of being swallowed (silent registration failure no longer looks like success)
- After `clearSubscriber()` / force logout, this device ignores stale IncomingCall FCM and declines unexpected WebSocket incoming calls via a persisted incoming-calls-enabled gate

### Changed

- `MicButton` requests `RECORD_AUDIO` when tapped without permission; if permanently denied, shows a settings dialog (copy from `CallStrings`) and treats the control as muted until access is granted
- After `clearSubscriber()`, `registerForIncomingCalls()` reconnects the existing Infobip RTC active connection (previously a no-op once `ActiveConnectionInfobipRTC` was created)
- `clearSubscriber()` always disconnects the active connection socket, including when Infobip reports `isConnected == false` (e.g. CONNECTING / RECONNECTING)
- `clearSubscriber()` dismisses incoming-call notification/ringtone, ends any local ringing/active call refs, and clears the persisted incoming-calls gate before disabling push
- Cached token is only invalidated when a `tokenProvider` can refill it (hosts without a provider keep the `registerSubscriber` / `updateToken` token)
- `CallSignalStrength` accepts optional `strongText` / `weakText` (falls back to `CallStrings.strongSignal` / `weakSignal`)
- `MicButton`, `SpeakerButton`, and `MessageButton` accept optional label text overrides (`label` / `activeLabel` on mic; `label` on speaker and message); unset values fall back to `CallStrings`
- `CallControlStyle` includes `label` / `activeLabel` for per-control label overrides on `CallControlBar`
- `registerForIncomingCalls(pushConfigId: String? = null)` accepts an optional push config ID override (falls back to `InfobipCallConfig.pushConfigId`)
- Blank `pushConfigId` skips FCM registration with a warning (foreground WebSocket only)
- Call status / duration UI now uses Infobip RTC duration instead of a local timer
- `clearSubscriber()` disconnects the foreground Infobip RTC active connection and disables push notifications on unregister
- Default call screens and components read branding from `CallAppearance` / `CallStrings` composition locals
- SDK manifest declares `VIBRATE` for incoming-call vibration
