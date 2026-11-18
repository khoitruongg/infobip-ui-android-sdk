package com.infobip.uisdk.model

data class CallSession(
    val id: String,
    val type: CallType = CallType.FreeCall,
    val state: CallState,
    val remoteParticipant: CallParticipant,
    /** Epoch millis when the call was established (`WebrtcCall.establishTime()`). */
    val startedAt: Long? = null,
    /** Epoch millis when the call was initiated (`WebrtcCall.startTime()`). */
    val startTime: Long? = null,
    /** Epoch millis when the call finished (`WebrtcCall.endTime()`). */
    val endTime: Long? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    /** Human-readable end/failure text (from Infobip status description). */
    val failureReason: String? = null,
    /**
     * Infobip hangup / error code for terminal states ([CallState.Ended] / [CallState.Failed]).
     * Prefer matching [CallEndCode.id] in host apps.
     */
    val endCode: CallEndCode? = null,
    /**
     * Whether this device or the remote/platform ended the call.
     * Useful with [CallEndCode.ID_NORMAL_HANGUP] / [CallEndCode.ID_CANCELLED].
     */
    val endedBy: CallEndedBy? = null,
    val audioRoutes: List<AudioRoute> = emptyList(),
    val activeAudioRouteId: String? = null,
    /**
     * Local device network quality for the active call (Infobip RTC).
     * `null` until seeded/updated. Map [CallNetworkQuality] for custom text, or use
     * [com.infobip.uisdk.ui.components.CallSignalStrength] for default strong/weak copy.
     */
    val localNetworkQuality: CallNetworkQuality? = null,
    /**
     * Remote participant network quality for the active call (Infobip RTC).
     * `null` until seeded/updated. Same usage as [localNetworkQuality].
     */
    val remoteNetworkQuality: CallNetworkQuality? = null,
)
