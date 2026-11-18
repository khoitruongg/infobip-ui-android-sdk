package com.infobip.uisdk

import kotlinx.coroutines.flow.StateFlow
import com.infobip.uisdk.model.AudioRoute
import com.infobip.uisdk.model.CallSession

/**
 * Main programmatic API for Infobip WebRTC calls.
 */
interface InfobipCallClient {

    val activeSession: StateFlow<CallSession?>

    /**
     * Live call duration in seconds from Infobip RTC (`WebrtcCall.duration()`).
     *
     * Safe to poll from custom UIs at any time (including Connecting / hangup / Failed):
     * returns `0` when there is no usable call and never throws if RTC tears down
     * the underlying call while the UI is still reading duration.
     */
    fun callDurationSeconds(): Int

    /**
     * Register the logged-in user. [token] must be obtained by the host app
     * (typically from your backend calling Infobip POST /webrtc/1/token).
     */
    suspend fun registerSubscriber(
        identity: String,
        displayName: String,
        token: String,
    )

    /** Replace the cached subscriber token (e.g. after refresh). */
    fun updateToken(token: String)

    /**
     * Clear subscriber state (e.g. on logout / force logout).
     *
     * Disconnects the foreground Infobip RTC active connection, disables push notifications,
     * dismisses any incoming-call UI, and clears the persisted incoming-calls gate so this device
     * ignores stale IncomingCall FCM until [registerForIncomingCalls] succeeds again.
     */
    fun clearSubscriber()

    /**
     * Register for foreground active connection and (optionally) push notifications.
     *
     * [pushConfigId] overrides [InfobipCallConfig.pushConfigId] for this registration when non-null
     * and non-blank. If both are blank, push registration is skipped (foreground WebSocket only)
     * and a warning is logged.
     *
     * On success, persists an incoming-calls-enabled flag so cold-start FCM is accepted.
     * Failures are propagated to the caller (they are not swallowed).
     */
    suspend fun registerForIncomingCalls(pushConfigId: String? = null)

    /** Returns true when [payload] was handled as an Infobip incoming call. */
    fun handlePushNotification(payload: Map<String, String>): Boolean

    /**
     * Start an outgoing WebRTC call.
     *
     * [destinationDisplayName] is shown on the outgoing call UI for the callee.
     * [customData] is forwarded to the callee and will be available in the incoming call event
     * (e.g. to pass `userName` / `avatarUrl`).
     */
    suspend fun startOutgoingCall(
        destinationIdentity: String,
        customData: Map<String, String> = emptyMap(),
        destinationDisplayName: String? = null,
    ): Result<CallSession>

    /**
     * Retries the most recent outgoing call (destination + custom data + display name).
     * Fails if no outgoing call has been placed since init / [clearSubscriber].
     */
    suspend fun retryLastCall(): Result<CallSession>

    suspend fun acceptIncomingCall()

    suspend fun declineIncomingCall()

    suspend fun hangUp()

    suspend fun setMuted(muted: Boolean)

    suspend fun setSpeakerOn(speakerOn: Boolean)

    /** Available audio output routes for the active call (may be empty when idle). */
    fun audioRoutes(): List<AudioRoute>

    /** Currently selected audio route, if any. */
    fun activeAudioRoute(): AudioRoute?

    /**
     * Selects an audio route by [AudioRoute.id].
     * Speaker / earpiece also keep [CallSession.isSpeakerOn] in sync.
     */
    suspend fun selectAudioRoute(id: String)

    /** Sends a DTMF digit (0-9, *, #) on the active call. */
    suspend fun sendDtmf(digit: String)
}
