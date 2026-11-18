package com.infobip.uisdk.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.infobip.uisdk.InfobipCallConfig
import com.infobip.uisdk.InfobipTokenProvider
import com.infobip.uisdk.internal.push.IncomingCallNotifier
import com.infobip.uisdk.internal.push.IncomingCallRingtonePlayer
import com.infobip.uisdk.model.AudioRoute
import com.infobip.uisdk.model.AudioRouteIds
import com.infobip.uisdk.model.AudioRouteType
import com.infobip.uisdk.model.CallEndCode
import com.infobip.uisdk.model.CallEndedBy
import com.infobip.uisdk.model.CallNetworkQuality
import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.model.CallSession
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallType
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.sdk.api.call.IncomingWebrtcCall
import com.infobip.webrtc.sdk.api.call.WebrtcCall
import com.infobip.webrtc.sdk.api.event.call.CallEarlyMediaEvent
import com.infobip.webrtc.sdk.api.event.call.CallEstablishedEvent
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.call.CallRingingEvent
import com.infobip.webrtc.sdk.api.event.call.ErrorEvent
import com.infobip.webrtc.sdk.api.event.call.ReconnectedEvent
import com.infobip.webrtc.sdk.api.event.call.ReconnectingEvent
import com.infobip.webrtc.sdk.api.event.call.RemoteDisconnectedEvent
import com.infobip.webrtc.sdk.api.event.call.RemoteReconnectedEvent
import com.infobip.webrtc.sdk.api.event.listener.IncomingCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.WebrtcCallEventListener
import com.infobip.webrtc.sdk.api.model.ErrorCode
import com.infobip.webrtc.sdk.api.model.device.AudioDevice
import com.infobip.webrtc.sdk.api.model.device.AudioDeviceType
import com.infobip.webrtc.sdk.api.model.network.NetworkQuality
import com.infobip.webrtc.sdk.api.options.WebrtcCallOptions
import com.infobip.webrtc.sdk.api.model.push.Status
import com.infobip.webrtc.sdk.api.request.CallWebrtcRequest
import com.infobip.webrtc.sdk.impl.DefaultInfobipRTC
import com.infobip.webrtc.sdk.impl.event.listener.DefaultWebrtcCallEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class InfobipRtcCallRepository(
    private val appContext: Context,
    private val config: InfobipCallConfig,
    private val subscriberIdentityStore: SubscriberIdentityStore,
) {
    private val _activeSession = MutableStateFlow<CallSession?>(null)
    val activeSession: StateFlow<CallSession?> = _activeSession.asStateFlow()

    private val infobipRtc: InfobipRTC
        get() = InfobipRTC.getInstance()

    private var activeCall: WebrtcCall? = null
    private var incomingCall: IncomingWebrtcCall? = null
    private var cachedToken: String? = null
    private var registeredIdentity: String? = null

    private var lastOutgoingDestination: String? = null
    private var lastOutgoingCustomData: Map<String, String> = emptyMap()
    private var lastOutgoingDisplayName: String? = null

    /** True when this device initiated hangup / decline; consumed in [onHangup]. */
    private var localHangupRequested: Boolean = false

    private val tokenProvider: InfobipTokenProvider?
        get() = config.tokenProvider

    fun registerSubscriber(identity: String, displayName: String, token: String) {
        require(identity.isNotBlank()) { "identity must not be blank." }
        require(token.isNotBlank()) { "token must not be blank." }
        subscriberIdentityStore.save(identity, displayName)
        cachedToken = token.trim()
        if (registeredIdentity != null && registeredIdentity != identity.trim()) {
            registeredIdentity = null
        }
    }

    fun updateToken(token: String) {
        require(token.isNotBlank()) { "token must not be blank." }
        cachedToken = token.trim()
    }

    fun clearSubscriber() {
        IncomingCallNotifier.dismiss(appContext)
        OutgoingCallWaitingRingtonePlayer.stop()

        localHangupRequested = true
        runCatching { incomingCall?.decline() }
        runCatching { activeCall?.hangup() }
        incomingCall = null
        activeCall = null
        _activeSession.value = null
        localHangupRequested = false

        // Clear first so any in-flight FCM is ignored even if disablePush fails.
        IncomingCallsEnabledStore.setEnabled(appContext, false)

        disconnectActiveConnection()
        val token = cachedToken
        if (!token.isNullOrBlank()) {
            runCatching {
                infobipRtc.disablePushNotification(token, appContext) { result ->
                    if (result.status == Status.SUCCESS) {
                        Log.i(TAG, "Successfully unregistered push notifications")
                    } else {
                        Log.w(
                            TAG,
                            "Failed to unregister push notifications: ${result.description}",
                        )
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "Failed to disable push notifications on clearSubscriber", error)
            }
        }
        subscriberIdentityStore.clear()
        cachedToken = null
        registeredIdentity = null
        lastOutgoingDestination = null
        lastOutgoingCustomData = emptyMap()
        lastOutgoingDisplayName = null
    }

    // Uses impl DefaultInfobipRTC.disconnect(); not exposed on public InfobipRTC.
    private fun disconnectActiveConnection() {
        runCatching {
            val rtc = infobipRtc as? DefaultInfobipRTC
            if (rtc == null) {
                Log.w(TAG, "Unable to disconnect active connection: unexpected InfobipRTC type")
                return
            }
            // Always call disconnect(): gateway may be CONNECTING/RECONNECTING where
            // isConnected() is false but a socket is still open.
            rtc.disconnect()
            Log.i(TAG, "Disconnected active connection socket")
        }.onFailure { error ->
            Log.w(TAG, "Failed to disconnect active connection socket", error)
        }
    }

    private fun createCallEventListener(
        label: String,
        forCall: () -> WebrtcCall?,
    ): WebrtcCallEventListener {
        return object : DefaultWebrtcCallEventListener() {
            private fun trackedCall(forCall: () -> WebrtcCall?): WebrtcCall? {
                val call = forCall()
                return if (isTrackedCall(call)) call else null
            }

            override fun onRinging(callRingingEvent: CallRingingEvent) {
                trackedCall(forCall) ?: return
                if (label == "outgoing") {
                    onCallStateChanged(CallState.Ringing)
                }
            }

            override fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent) {
                trackedCall(forCall) ?: return
                // Remote early media / ringback is on the call; stop local waiting tone.
                OutgoingCallWaitingRingtonePlayer.stop()
            }

            override fun onEstablished(callEstablishedEvent: CallEstablishedEvent) {
                trackedCall(forCall) ?: return
                OutgoingCallWaitingRingtonePlayer.stop()
                onCallStateChanged(CallState.Active)
                syncAudioRoutesOnSession()
            }

            override fun onHangup(callHangupEvent: CallHangupEvent) {
                trackedCall(forCall) ?: return
                IncomingCallNotifier.dismiss(appContext)
                OutgoingCallWaitingRingtonePlayer.stop()

                val errorCode = callHangupEvent.errorCode
                val endCode = errorCode?.toCallEndCode()
                    ?: CallEndCode.from(
                        id = CallEndCode.ID_NORMAL_HANGUP,
                        name = "NORMAL_HANGUP",
                        description = "The call has ended.",
                    )
                val endedBy = if (localHangupRequested) CallEndedBy.Local else CallEndedBy.Remote
                localHangupRequested = false

                val terminalState = terminalStateFor(endCode)
                onCallTerminated(
                    state = terminalState,
                    endCode = endCode,
                    endedBy = endedBy,
                )

                // Keep the terminal session so UI can observe endCode / endedBy.
                // Clearing immediately would conflate away the update on StateFlow.
                activeCall = null
                incomingCall = null
            }

            override fun onError(errorEvent: ErrorEvent) {
                trackedCall(forCall) ?: return
                val errorCode = errorEvent.errorCode
                val endCode = errorCode?.toCallEndCode()
                    ?: CallEndCode.from(
                        id = CallEndCode.ID_UNKNOWN,
                        name = "UNKNOWN",
                        description = "An unexpected call error occurred.",
                    )
                val endedBy = if (localHangupRequested) CallEndedBy.Local else CallEndedBy.Remote
                localHangupRequested = false
                onCallTerminated(
                    state = CallState.Failed,
                    endCode = endCode,
                    endedBy = endedBy,
                )
            }

            override fun onReconnecting(reconnectingEvent: ReconnectingEvent) {
                trackedCall(forCall)
            }

            override fun onReconnected(reconnectedEvent: ReconnectedEvent) {
                trackedCall(forCall)
            }

            override fun onRemoteDisconnected(remoteDisconnectedEvent: RemoteDisconnectedEvent) {
                trackedCall(forCall)
            }

            override fun onRemoteReconnected(remoteReconnectedEvent: RemoteReconnectedEvent) {
                trackedCall(forCall)
            }
        }
    }

    private fun isTrackedCall(call: WebrtcCall?): Boolean =
        call != null && (activeCall === call || incomingCall === call)

    private fun attachNetworkQualityListeners(call: WebrtcCall) {
        call.setNetworkQualityEventListener { event ->
            if (!isTrackedCall(call)) return@setNetworkQualityEventListener
            val quality = event.networkQuality?.toCallNetworkQuality() ?: return@setNetworkQualityEventListener
            Log.d(TAG, "localNetworkQuality → $quality")
            _activeSession.update { session ->
                session?.copy(localNetworkQuality = quality)
            }
        }
        call.setRemoteNetworkQualityEventListener { event ->
            if (!isTrackedCall(call)) return@setRemoteNetworkQualityEventListener
            val quality = event.networkQuality?.toCallNetworkQuality() ?: return@setRemoteNetworkQualityEventListener
            Log.d(TAG, "remoteNetworkQuality → $quality")
            _activeSession.update { session ->
                session?.copy(remoteNetworkQuality = quality)
            }
        }
    }

    private fun NetworkQuality.toCallNetworkQuality(): CallNetworkQuality =
        CallNetworkQuality.fromScore(score)

    private val incomingCallEventListener = IncomingCallEventListener { event ->
        val call = event.incomingWebrtcCall
        if (!IncomingCallsEnabledStore.isEnabled(appContext)) {
            Log.i(TAG, "Declining incoming call: subscriber cleared on this device")
            runCatching { call.decline() }
            return@IncomingCallEventListener
        }

        incomingCall = call
        call.eventListener = createCallEventListener("incoming") { call }
        attachNetworkQualityListeners(call)

        val source = call.source()
        val identity = source.identifier().orEmpty()
        val customData = event.customData.orEmpty()
        val customUserName = customData["userName"]?.takeIf { it.isNotBlank() }
        val customAvatarUrl = customData["avatarUrl"]?.takeIf { it.isNotBlank() }

        val displayName = customUserName ?: source.displayIdentifier()
            ?.takeIf { it.isNotBlank() }
            ?: identity

        showIncomingCallNotificationIfAllowed(appContext, displayName)
        onIncomingCallReceived(
            CallParticipant.fromIdentity(identity, displayName, avatarUrl = customAvatarUrl),
        )
    }

    private suspend fun obtainSubscriberToken(): String {
        val identity = subscriberIdentityStore.getIdentity()
            ?: error("Call registerSubscriber() before making or receiving calls.")

        cachedToken?.let { return it }

        val provider = tokenProvider
            ?: error("No cached token. Call registerSubscriber() with a token first.")

        return provider.getToken(identity).also { token ->
            require(token.isNotBlank()) { "Token provider returned a blank token." }
            cachedToken = token
        }
    }

    /**
     * Drops the cached token only when [tokenProvider] can refill it.
     * Without a provider, keep the token from [registerSubscriber] / [updateToken].
     */
    private fun invalidateCachedTokenForRefresh() {
        if (tokenProvider != null) {
            cachedToken = null
        }
    }

    suspend fun registerForIncomingCalls(pushConfigId: String? = null) {
        val identity = subscriberIdentityStore.getIdentity()
            ?: error("Call registerSubscriber() before registering for incoming calls.")

        try {
            registerForPushNotifications(pushConfigId)
            registerForActiveConnection()
            registeredIdentity = identity
            IncomingCallsEnabledStore.setEnabled(appContext, true)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Failed to register for incoming calls", error)
            throw error
        }
    }

    private suspend fun registerForPushNotifications(pushConfigId: String? = null) {
        val resolvedPushConfigId = pushConfigId?.trim()?.takeIf { it.isNotBlank() }
            ?: config.pushConfigId.trim()
        if (resolvedPushConfigId.isBlank()) {
            Log.w(
                TAG,
                "pushConfigId is blank; skipping FCM push registration. " +
                    "Background/killed incoming calls will not work until a non-blank pushConfigId is provided.",
            )
            return
        }

        val token = obtainSubscriberToken()

        suspendCancellableCoroutine { continuation ->
            infobipRtc.enablePushNotification(token, appContext, resolvedPushConfigId) { result ->
                if (result.status == Status.SUCCESS) {
                    continuation.resume(Unit)
                } else {
                    val message = result.description ?: "Failed to register for push notifications."
                    continuation.resumeWithException(IllegalStateException(message))
                }
            }
        }
    }

    private suspend fun registerForActiveConnection() {
        val token = obtainSubscriberToken()
        val rtc = InfobipRTC.getInstance()
        // SDK only creates ActiveConnectionInfobipRTC when the singleton is still
        // PushInfobipRTC. After the first register, re-register is a no-op unless we
        // reconnect the existing instance (e.g. after clearSubscriber()).
        // Use reflection for ActiveConnectionInfobipRTC / AccessToken to avoid
        // Kotlin analysis crashes from Infobip RTC impl bytecode.
        if (isActiveConnectionRtc(rtc)) {
            val defaultRtc = rtc as? DefaultInfobipRTC
            if (defaultRtc == null) {
                Log.w(TAG, "Unable to reconnect active connection: unexpected InfobipRTC type")
                return
            }
            bindIncomingCallListener(defaultRtc)
            if (defaultRtc.isConnected) {
                Log.i(TAG, "InfobipRTC active connection already online")
                return
            }
            // Give the previous socket a moment to finish closing after clearSubscriber().
            delay(RECONNECT_DELAY_MS)
            Log.i(TAG, "Reconnecting InfobipRTC active connection after unregister")
            reconnectActiveConnection(defaultRtc, token)
        } else {
            rtc.registerForActiveConnection(token, appContext, incomingCallEventListener)
        }
    }

    private fun isActiveConnectionRtc(rtc: InfobipRTC): Boolean =
        rtc.javaClass.name == ACTIVE_CONNECTION_RTC_CLASS

    private fun bindIncomingCallListener(rtc: DefaultInfobipRTC) {
        runCatching {
            val field = DefaultInfobipRTC::class.java.getDeclaredField("incomingCallEventListener")
            field.isAccessible = true
            field.set(rtc, incomingCallEventListener)
        }.onFailure { error ->
            Log.w(TAG, "Failed to bind incoming call listener on reconnect", error)
        }
    }

    // DefaultInfobipRTC.connect(AccessToken) is protected; reconnect after unregister needs it.
    private fun reconnectActiveConnection(rtc: DefaultInfobipRTC, token: String) {
        val accessTokenClass = Class.forName(ACCESS_TOKEN_CLASS)
        val accessToken = accessTokenClass.getConstructor(String::class.java).newInstance(token)
        val connect = DefaultInfobipRTC::class.java.getDeclaredMethod("connect", accessTokenClass)
        connect.isAccessible = true
        connect.invoke(rtc, accessToken)
    }

    fun handleIncomingPushNotification(payload: Map<String, String>): Boolean {
        if (!infobipRtc.isIncomingCall(payload)) {
            return false
        }
        if (!IncomingCallsEnabledStore.isEnabled(appContext)) {
            Log.i(TAG, "Ignoring incoming-call push: subscriber cleared on this device")
            return false
        }

        infobipRtc.handleIncomingCall(payload, appContext, incomingCallEventListener)
        return true
    }

    private suspend fun disconnectExistingSession(clearActiveSession: Boolean = true) {
        OutgoingCallWaitingRingtonePlayer.stop()
        if (activeCall != null || incomingCall != null) {
            localHangupRequested = true
        }
        runCatching { activeCall?.hangup() }
        runCatching { incomingCall?.decline() }
        runCatching { infobipRtc.getActiveCall()?.hangup() }
        runCatching { infobipRtc.getActiveApplicationCall()?.hangup() }
        runCatching { infobipRtc.getActiveRoomCall()?.leave() }

        waitForCallTeardown()

        activeCall = null
        incomingCall = null
        if (clearActiveSession) {
            _activeSession.value = null
            localHangupRequested = false
        }
    }

    private suspend fun waitForCallTeardown() {
        val deadline = System.currentTimeMillis() + CALL_TEARDOWN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val sdkHasActiveSession = infobipRtc.getActiveCall() != null ||
                infobipRtc.getActiveApplicationCall() != null ||
                infobipRtc.getActiveRoomCall() != null
            if (!sdkHasActiveSession) {
                return
            }
            delay(CALL_TEARDOWN_POLL_MS)
        }
    }

    private suspend fun placeOutgoingCall(
        callerIdentity: String,
        destinationIdentity: String,
        customData: Map<String, String>,
        destinationDisplayName: String? = null,
    ): CallSession {
        val token = obtainSubscriberToken()
        val sessionId = UUID.randomUUID().toString()
        val session = CallSession(
            id = sessionId,
            type = CallType.FreeCall,
            state = CallState.Connecting,
            remoteParticipant = CallParticipant.fromIdentity(
                identity = destinationIdentity,
                fallbackDisplayName = destinationDisplayName,
            ),
        )
        localHangupRequested = false
        _activeSession.value = session
        // Start here so custom outgoing UIs (not only OutgoingCallScreen) hear ringback.
        OutgoingCallWaitingRingtonePlayer.start(appContext)

        val outboundCallRef = arrayOf<WebrtcCall?>(null)
        val callWebrtcRequest = CallWebrtcRequest(
            token,
            appContext,
            destinationIdentity,
            createCallEventListener("outgoing") { outboundCallRef[0] },
        )
        outboundCallRef[0] = if (customData.isNotEmpty()) {
            val options = WebrtcCallOptions.builder()
                .customData(customData)
                .build()
            infobipRtc.callWebrtc(callWebrtcRequest, options)
        } else {
            infobipRtc.callWebrtc(callWebrtcRequest)
        }
        activeCall = outboundCallRef[0]
        outboundCallRef[0]?.let(::attachNetworkQualityListeners)
        return session
    }

    suspend fun startOutgoingCall(
        destinationIdentity: String,
        customData: Map<String, String>,
        destinationDisplayName: String? = null,
    ): Result<CallSession> {
        val callerIdentity = subscriberIdentityStore.getIdentity()
            ?: return Result.failure(
                IllegalStateException("Call registerSubscriber() before making calls."),
            )

        val calleeIdentity = destinationIdentity.trim()
        val calleeDisplayName = destinationDisplayName?.trim()?.takeIf { it.isNotBlank() }
        if (calleeIdentity.isBlank()) {
            return Result.failure(IllegalArgumentException("Callee identity must not be blank."))
        }
        if (callerIdentity == calleeIdentity) {
            return Result.failure(
                IllegalArgumentException("Cannot call yourself. Use a different callee identity."),
            )
        }

        lastOutgoingDestination = calleeIdentity
        lastOutgoingCustomData = customData
        lastOutgoingDisplayName = calleeDisplayName

        return runCatching {
            disconnectExistingSession(clearActiveSession = false)
            // registeredIdentity is only set after registerForIncomingCalls(); do not treat
            // null as a reason to drop the token from registerSubscriber().
            if (registeredIdentity != null && registeredIdentity != callerIdentity) {
                registeredIdentity = null
                invalidateCachedTokenForRefresh()
            }
            placeOutgoingCall(callerIdentity, calleeIdentity, customData, calleeDisplayName)
        }.recoverCatching { error ->
            if (error is CancellationException) throw error
            disconnectExistingSession(clearActiveSession = false)
            registeredIdentity = null
            invalidateCachedTokenForRefresh()
            delay(RECONNECT_DELAY_MS)
            placeOutgoingCall(callerIdentity, calleeIdentity, customData, calleeDisplayName)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            activeCall = null
            _activeSession.value = null
        }
    }

    suspend fun retryLastCall(): Result<CallSession> {
        val destination = lastOutgoingDestination
            ?: return Result.failure(
                IllegalStateException("No previous outgoing call to retry."),
            )
        return startOutgoingCall(
            destinationIdentity = destination,
            customData = lastOutgoingCustomData,
            destinationDisplayName = lastOutgoingDisplayName,
        )
    }

    fun callDurationSeconds(): Int =
        // Infobip RTC DefaultCall.duration() NPEs when inner ApplicationCall is null
        // (common during hangup/teardown while UI still polls).
        runCatching { activeCall?.duration() ?: incomingCall?.duration() ?: 0 }
            .getOrDefault(0)

    suspend fun acceptIncomingCall() {
        val call = incomingCall ?: return
        IncomingCallNotifier.dismiss(appContext)
        call.accept()
        activeCall = call
        incomingCall = null
        // Accept sets Active before onEstablished; seed Excellent for the same reason as
        // onCallStateChanged (Infobip only notifies on quality *changes*).
        _activeSession.update { session ->
            session?.withCallTiming(call)?.copy(
                state = CallState.Active,
                localNetworkQuality = session.localNetworkQuality ?: CallNetworkQuality.Excellent,
                remoteNetworkQuality = session.remoteNetworkQuality ?: CallNetworkQuality.Excellent,
            )
        }
        syncAudioRoutesOnSession()
    }

    suspend fun declineIncomingCall() {
        IncomingCallNotifier.dismiss(appContext)
        val trackedIncomingCall = incomingCall
        if (trackedIncomingCall != null) {
            localHangupRequested = true
            trackedIncomingCall.decline()
            // Prefer keeping a terminal session if onHangup arrives; otherwise set locally.
            if (_activeSession.value?.state == CallState.Incoming) {
                onCallTerminated(
                    state = CallState.Failed,
                    endCode = CallEndCode.from(
                        id = CallEndCode.ID_REJECTED,
                        name = "REJECTED",
                        description = "The call was declined.",
                    ),
                    endedBy = CallEndedBy.Local,
                )
            }
            incomingCall = null
            return
        }

        if (activeCall != null) {
            hangUp()
            return
        }

        _activeSession.value = null
    }

    suspend fun hangUp() {
        localHangupRequested = true
        OutgoingCallWaitingRingtonePlayer.stop()

        val hadCall = activeCall != null || incomingCall != null
        runCatching { activeCall?.hangup() }
        runCatching { incomingCall?.decline() }

        if (!hadCall) {
            onCallTerminated(
                state = CallState.Failed,
                endCode = CallEndCode.from(
                    id = CallEndCode.ID_CANCELLED,
                    name = "CANCELLED",
                    description = "The call was cancelled.",
                ),
                endedBy = CallEndedBy.Local,
            )
            localHangupRequested = false
            return
        }

        // Optimistic terminal state so clients see Local cancel/hangup immediately.
        // onHangup will refine endCode from Infobip and clear call refs.
        val current = _activeSession.value
        if (
            current != null &&
            current.state != CallState.Ended &&
            current.state != CallState.Failed
        ) {
            val wasConnected = current.state == CallState.Active
            val endCode = if (wasConnected) {
                CallEndCode.from(
                    id = CallEndCode.ID_NORMAL_HANGUP,
                    name = "NORMAL_HANGUP",
                    description = "The call has ended.",
                )
            } else {
                CallEndCode.from(
                    id = CallEndCode.ID_CANCELLED,
                    name = "CANCELLED",
                    description = "The call was cancelled.",
                )
            }
            onCallTerminated(
                state = if (wasConnected) CallState.Ended else CallState.Failed,
                endCode = endCode,
                endedBy = CallEndedBy.Local,
            )
        }
    }

    suspend fun setMuted(muted: Boolean) {
        activeCall?.mute(muted) ?: incomingCall?.mute(muted)
        _activeSession.update { session ->
            session?.copy(isMuted = muted)
        }
    }

    suspend fun setSpeakerOn(speakerOn: Boolean) {
        activeCall?.speakerphone(speakerOn) ?: incomingCall?.speakerphone(speakerOn)
        syncAudioRoutesOnSession(speakerOn = speakerOn)
    }

    fun audioRoutes(): List<AudioRoute> = mapAudioRoutes()

    fun activeAudioRoute(): AudioRoute? = audioRoutes().firstOrNull { it.isActive }

    suspend fun selectAudioRoute(id: String) {
        val call = activeCall ?: incomingCall ?: return
        val manager = call.audioDeviceManager()
        val device = manager.availableAudioDevices.firstOrNull { it.toRouteId() == id }
            ?: when (id) {
                AudioRouteIds.SPEAKER -> AudioDevice.SPEAKER
                AudioRouteIds.EARPIECE -> AudioDevice.EARPIECE
                else -> null
            }
            ?: return

        runCatching { manager.selectAudioDevice(device) }
        val speakerOn = device.audioDeviceType == AudioDeviceType.SPEAKER
        runCatching { call.speakerphone(speakerOn) }
        syncAudioRoutesOnSession(speakerOn = speakerOn)
    }

    suspend fun sendDtmf(digit: String) {
        val sanitized = digit.trim()
        if (sanitized.isEmpty()) return
        activeCall?.sendDTMF(sanitized) ?: incomingCall?.sendDTMF(sanitized)
    }

    private fun syncAudioRoutesOnSession(speakerOn: Boolean? = null) {
        val routes = mapAudioRoutes()
        val activeId = routes.firstOrNull { it.isActive }?.id
            ?: if (speakerOn == true) AudioRouteIds.SPEAKER else AudioRouteIds.EARPIECE
        val resolvedSpeaker = speakerOn
            ?: (activeId == AudioRouteIds.SPEAKER)
        _activeSession.update { session ->
            session?.copy(
                isSpeakerOn = resolvedSpeaker,
                audioRoutes = routes,
                activeAudioRouteId = activeId,
            )
        }
    }

    private fun mapAudioRoutes(): List<AudioRoute> {
        val call = activeCall ?: incomingCall ?: return emptyList()
        val manager = runCatching { call.audioDeviceManager() }.getOrNull() ?: return emptyList()
        val active = runCatching { manager.activeDevice }.getOrNull()
        val available = runCatching { manager.availableAudioDevices }.getOrNull().orEmpty()
        if (available.isEmpty()) {
            val speakerOn = runCatching { call.speakerphone() }.getOrDefault(false)
            return listOf(
                AudioRoute(
                    id = AudioRouteIds.EARPIECE,
                    name = AudioDeviceType.EARPIECE.name.lowercase().replaceFirstChar { it.titlecase() },
                    type = AudioRouteType.Earpiece,
                    isActive = !speakerOn,
                ),
                AudioRoute(
                    id = AudioRouteIds.SPEAKER,
                    name = AudioDeviceType.SPEAKER.name.lowercase().replaceFirstChar { it.titlecase() },
                    type = AudioRouteType.Speaker,
                    isActive = speakerOn,
                ),
            )
        }
        return available.map { device ->
            AudioRoute(
                id = device.toRouteId(),
                name = device.name,
                type = device.audioDeviceType.toRouteType(),
                isActive = active != null && active == device,
            )
        }
    }

    private fun AudioDevice.toRouteId(): String = when (audioDeviceType) {
        AudioDeviceType.EARPIECE -> AudioRouteIds.EARPIECE
        AudioDeviceType.SPEAKER -> AudioRouteIds.SPEAKER
        AudioDeviceType.BLUETOOTH -> "${AudioRouteIds.BLUETOOTH}:${name}"
        AudioDeviceType.WIRED_HEADSET -> "${AudioRouteIds.HEADPHONES}:${name}"
    }

    private fun AudioDeviceType.toRouteType(): AudioRouteType = when (this) {
        AudioDeviceType.EARPIECE -> AudioRouteType.Earpiece
        AudioDeviceType.SPEAKER -> AudioRouteType.Speaker
        AudioDeviceType.BLUETOOTH -> AudioRouteType.Bluetooth
        AudioDeviceType.WIRED_HEADSET -> AudioRouteType.Headphones
    }

    private fun onIncomingCallReceived(participant: CallParticipant) {
        runCatching { IncomingCallRingtonePlayer.start(appContext) }
            .onFailure { Log.w(TAG, "Failed to start incoming call ringtone", it) }
        localHangupRequested = false

        _activeSession.value = CallSession(
            id = UUID.randomUUID().toString(),
            type = CallType.FreeCall,
            state = CallState.Incoming,
            remoteParticipant = participant,
        )
    }

    private fun onCallStateChanged(state: CallState, failureReason: String? = null) {
        if (
            state == CallState.Active ||
            state == CallState.Ended ||
            state == CallState.Failed
        ) {
            OutgoingCallWaitingRingtonePlayer.stop()
        }

        val call = activeCall ?: incomingCall
        _activeSession.update { session ->
            session
                ?.withCallTiming(call)
                ?.copy(
                    state = state,
                    failureReason = failureReason,
                    endCode = if (state == CallState.Ended || state == CallState.Failed) {
                        session.endCode
                    } else {
                        null
                    },
                    endedBy = if (state == CallState.Ended || state == CallState.Failed) {
                        session.endedBy
                    } else {
                        null
                    },
                    // Infobip RTC only emits network-quality events on *change* from an
                    // Excellent baseline, so seed Excellent when the call becomes Active.
                    localNetworkQuality = if (state == CallState.Active) {
                        session.localNetworkQuality ?: CallNetworkQuality.Excellent
                    } else {
                        session.localNetworkQuality
                    },
                    remoteNetworkQuality = if (state == CallState.Active) {
                        session.remoteNetworkQuality ?: CallNetworkQuality.Excellent
                    } else {
                        session.remoteNetworkQuality
                    },
                )
        }
    }

    private fun onCallTerminated(
        state: CallState,
        endCode: CallEndCode,
        endedBy: CallEndedBy,
    ) {
        val failureReason = formatFailureReason(endCode)
        Log.w(
            TAG,
            "Call terminated. state=$state endCode=${endCode.name}(${endCode.id}) " +
                "endedBy=$endedBy failureReason=$failureReason",
        )
        val call = activeCall ?: incomingCall
        _activeSession.update { session ->
            session
                ?.withCallTiming(call)
                ?.copy(
                    state = state,
                    failureReason = failureReason,
                    endCode = endCode,
                    endedBy = endedBy,
                )
        }
    }

    private fun CallSession.withCallTiming(call: WebrtcCall?): CallSession {
        if (call == null) return this
        // Same ApplicationCall-null hazard as duration() on hangup/teardown.
        return copy(
            startTime = runCatching { call.startTime()?.time }.getOrNull() ?: startTime,
            startedAt = runCatching { call.establishTime()?.time }.getOrNull() ?: startedAt,
            endTime = runCatching { call.endTime()?.time }.getOrNull() ?: endTime,
        )
    }

    private fun ErrorCode.toCallEndCode(): CallEndCode =
        CallEndCode.from(id = id, name = name, description = description)

    private fun terminalStateFor(endCode: CallEndCode): CallState =
        when (endCode.id) {
            CallEndCode.ID_NORMAL_HANGUP,
            CallEndCode.ID_ANSWERED_ELSEWHERE,
            CallEndCode.ID_MACHINE_DETECTED,
            CallEndCode.ID_HUMAN_DETECTED,
            -> CallState.Ended

            else -> CallState.Failed
        }

    private fun formatFailureReason(endCode: CallEndCode): String {
        if (
            endCode.id == CallEndCode.ID_BUSY ||
            endCode.id == CallEndCode.ID_NO_ANSWER ||
            endCode.id == CallEndCode.ID_REJECTED ||
            endCode.id == CallEndCode.ID_CANCELLED ||
            endCode.id == CallEndCode.ID_DESTINATION_UNAVAILABLE ||
            endCode.id == CallEndCode.ID_DESTINATION_NOT_FOUND ||
            endCode.id == CallEndCode.ID_NORMAL_HANGUP
        ) {
            return endCode.description
        }
        val base = "${endCode.name} (${endCode.id}): ${endCode.description}"
        if (endCode.id == CallEndCode.ID_FORBIDDEN) {
            return "$base. The callee must register for incoming calls on their device."
        }
        return base
    }

    @SuppressLint("MissingPermission")
    private fun showIncomingCallNotificationIfAllowed(context: Context, displayName: String) {
        if (!canPostIncomingCallNotification(context)) return
        runCatching {
            IncomingCallNotifier.show(context, displayName)
        }
    }

    private fun canPostIncomingCallNotification(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private companion object {
        const val TAG = "InfobipRtcCallRepository"
        const val ACTIVE_CONNECTION_RTC_CLASS =
            "com.infobip.webrtc.sdk.impl.ActiveConnectionInfobipRTC"
        const val ACCESS_TOKEN_CLASS = "com.infobip.webrtc.sdk.impl.util.AccessToken"
        const val RECONNECT_DELAY_MS = 500L
        const val CALL_TEARDOWN_TIMEOUT_MS = 3_000L
        const val CALL_TEARDOWN_POLL_MS = 50L
    }
}
