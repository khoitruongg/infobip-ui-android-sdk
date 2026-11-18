package com.infobip.uisdk.ui.screens.incoming

import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallType

internal data class IncomingCallUiState(
    val callType: CallType = CallType.FreeCall,
    val statusText: String = "Calling you",
    val remoteParticipant: CallParticipant = CallParticipant(
        identity = "",
        displayName = "",
    ),
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val callState: CallState = CallState.Incoming,
) {
    val callControlsEnabled: Boolean
        get() = callState == CallState.Active

    val showAcceptButton: Boolean
        get() = callState == CallState.Incoming
}
