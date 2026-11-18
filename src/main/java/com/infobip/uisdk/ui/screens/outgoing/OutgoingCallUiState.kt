package com.infobip.uisdk.ui.screens.outgoing

import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallType

internal data class OutgoingCallUiState(
    val callType: CallType = CallType.FreeCall,
    val statusText: String = "Connecting...",
    val remoteParticipant: CallParticipant = CallParticipant(
        identity = "",
        displayName = "",
    ),
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val callState: CallState = CallState.Connecting,
) {
    val callControlsEnabled: Boolean
        get() = callState == CallState.Connecting ||
            callState == CallState.Ringing ||
            callState == CallState.Active

    val isWaitingForReceiver: Boolean
        get() = callState == CallState.Connecting || callState == CallState.Ringing
}
