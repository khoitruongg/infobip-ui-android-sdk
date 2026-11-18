package com.infobip.uisdk.example.customui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.model.CallSession
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallStatusUiModel
import com.infobip.uisdk.model.CallType
import com.infobip.uisdk.ui.components.CallAvatar
import com.infobip.uisdk.ui.components.CallBackground
import com.infobip.uisdk.ui.components.CallControlBar
import com.infobip.uisdk.ui.components.CallSignalStrength
import com.infobip.uisdk.ui.components.CallStatus
import com.infobip.uisdk.ui.components.HangUpCallButton
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings
import kotlinx.coroutines.delay

/**
 * Example of a fully custom outgoing call screen built from the SDK building blocks.
 *
 * This intentionally does not use `OutgoingCallScreen` (default SDK screen) or its internal ViewModel.
 */
@Composable
fun CustomOutgoingCallScreen(
    client: InfobipCallClient,
    destinationIdentity: String,
    destinationDisplayName: String? = null,
    customData: Map<String, String> = emptyMap(),
    onCallEnded: () -> Unit,
    onCallFailed: () -> Unit = onCallEnded,
    modifier: Modifier = Modifier,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    var hadActiveSession by remember(destinationIdentity, destinationDisplayName, customData) {
        mutableStateOf(false)
    }
    var wasConnected by remember(destinationIdentity, destinationDisplayName, customData) {
        mutableStateOf(false)
    }

    LaunchedEffect(destinationIdentity, destinationDisplayName, customData) {
        client.startOutgoingCall(
            destinationIdentity = destinationIdentity,
            customData = customData,
            destinationDisplayName = destinationDisplayName,
        ).onFailure {
            onCallFailed()
        }
    }

    val session by client.activeSession.collectAsStateWithLifecycle()
    val currentSession = session
    val state = currentSession?.state

    // Retain the last non-null session so a cleared activeSession does not flash Connecting.
    var lastSession by remember(destinationIdentity, destinationDisplayName, customData) {
        mutableStateOf<CallSession?>(null)
    }
    if (currentSession != null) {
        hadActiveSession = true
        lastSession = currentSession
        if (currentSession.state == CallState.Active) {
            wasConnected = true
        }
    }

    LaunchedEffect(state, currentSession?.failureReason, currentSession?.endCode, currentSession?.endedBy) {
        when (state) {
            CallState.Ended -> {
                Log.d(
                    "CustomOutgoingCallScreen",
                    "Call ended. endCode=${currentSession?.endCode?.name}" +
                        "(${currentSession?.endCode?.id}) endedBy=${currentSession?.endedBy}",
                )
                delay(TERMINAL_CALL_DISMISS_DELAY_MS)
                onCallEnded()
            }
            CallState.Failed -> {
                Log.w(
                    "CustomOutgoingCallScreen",
                    "Call failed. endCode=${currentSession?.endCode?.name}" +
                        "(${currentSession?.endCode?.id}) endedBy=${currentSession?.endedBy} " +
                        "failureReason=${currentSession?.failureReason.orEmpty()}",
                )
                delay(TERMINAL_CALL_DISMISS_DELAY_MS)
                onCallFailed()
            }
            else -> Unit
        }
    }

    LaunchedEffect(currentSession) {
        if (hadActiveSession && currentSession == null) {
            val lastState = lastSession?.state
            if (lastState != CallState.Ended && lastState != CallState.Failed) {
                // Some paths clear the session without emitting Failed/Ended.
                delay(TERMINAL_CALL_DISMISS_DELAY_MS)
                onCallFailed()
            }
        }
    }

    val displaySession = currentSession
        ?: lastSession
        ?: CallSession(
            id = destinationIdentity,
            type = CallType.FreeCall,
            state = CallState.Connecting,
            remoteParticipant = CallParticipant.fromIdentity(
                identity = destinationIdentity,
                fallbackDisplayName = destinationDisplayName,
            ),
        )

    var durationSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(displaySession.state) {
        if (displaySession.state != CallState.Active) return@LaunchedEffect
        while (true) {
            durationSeconds = client.callDurationSeconds()
            delay(1_000)
        }
    }

    val displayStatus = when (displaySession.state) {
        CallState.Active -> formatElapsedTime(durationSeconds)
        CallState.Failed -> displaySession.failureReason
            ?.takeIf { it.isNotBlank() && it != strings.callFailed }
            ?: strings.outgoingStatusFor(displaySession.state, wasConnected = wasConnected)
        else -> strings.outgoingStatusFor(
            state = displaySession.state,
            wasConnected = wasConnected,
            failureReason = displaySession.failureReason,
            durationText = null,
        )
    }
    

    CallBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = appearance.metrics.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(
                modifier = Modifier.height(
                    appearance.metrics.controlToActionsSpacing - appearance.metrics.controlSpacing,
                ),
            )
            CallStatus(
                status = CallStatusUiModel(
                    callTypeLabel = strings.freeCallTitle,
                    callState = displaySession.state,
                    statusText = displayStatus,
                ),
            )

            Spacer(modifier = Modifier.weight(1f))

            CallAvatar(participant = displaySession.remoteParticipant)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = displaySession.remoteParticipant.displayName,
                style = appearance.fonts.name,
                color = appearance.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Default strong/weak text. For per-level copy, map displaySession.localNetworkQuality yourself.
            CallSignalStrength(client = client)

            Spacer(modifier = Modifier.weight(1f))

            CallControlBar(
                client = client,
                enabled = displaySession.state == CallState.Active,
                showSpeaker = true,
                showMic = true,
                showMessage = true,
                onMessageClick = { /* host handles chat */ },
            )

            Spacer(modifier = Modifier.height(appearance.metrics.controlToActionsSpacing))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = appearance.metrics.actionRowHorizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HangUpCallButton(client = client)
            }

            Spacer(modifier = Modifier.height(appearance.metrics.bottomSpacing))
        }
    }
}

private const val TERMINAL_CALL_DISMISS_DELAY_MS = 2_000L
