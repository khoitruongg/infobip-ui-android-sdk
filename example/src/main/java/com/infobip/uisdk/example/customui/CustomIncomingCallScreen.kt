package com.infobip.uisdk.example.customui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallStatusUiModel
import com.infobip.uisdk.ui.components.AcceptCallButton
import com.infobip.uisdk.ui.components.CallAvatar
import com.infobip.uisdk.ui.components.CallBackground
import com.infobip.uisdk.ui.components.CallControlBar
import com.infobip.uisdk.ui.components.CallSignalStrength
import com.infobip.uisdk.ui.components.CallStatus
import com.infobip.uisdk.ui.components.DeclineCallButton
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings
import kotlinx.coroutines.delay

/**
 * Example of a fully custom incoming call screen built from the SDK building blocks.
 *
 * This intentionally does not use `IncomingCallScreen` (default SDK screen) or its internal ViewModel.
 */
@Composable
fun CustomIncomingCallScreen(
    client: InfobipCallClient,
    onCallEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session by client.activeSession.collectAsStateWithLifecycle()
    val currentSession = session
    val appearance = currentCallAppearance
    val strings = currentCallStrings

    LaunchedEffect(currentSession?.state) {
        if (
            currentSession == null ||
            currentSession.state == CallState.Ended ||
            currentSession.state == CallState.Failed
        ) {
            onCallEnded()
        }
    }

    if (currentSession == null) return

    var durationSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentSession.state) {
        if (currentSession.state != CallState.Active) return@LaunchedEffect
        while (true) {
            durationSeconds = client.callDurationSeconds()
            delay(1_000)
        }
    }

    val displayStatus = when (currentSession.state) {
        CallState.Active -> formatElapsedTime(durationSeconds)
        else -> strings.statusFor(
            state = currentSession.state,
            failureReason = currentSession.failureReason,
            incoming = true,
        )
    }
    val showAcceptButton = currentSession.state == CallState.Incoming

    CallBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = appearance.metrics.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            CallStatus(
                status = CallStatusUiModel(
                    callTypeLabel = strings.freeCallTitle,
                    callState = currentSession.state,
                    statusText = displayStatus,
                ),
            )

            Spacer(modifier = Modifier.weight(1f))

            CallAvatar(participant = currentSession.remoteParticipant)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = currentSession.remoteParticipant.displayName,
                style = appearance.fonts.name,
                color = appearance.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Default strong/weak text. For per-level copy, map currentSession.localNetworkQuality yourself.
            CallSignalStrength(client = client)

            Spacer(modifier = Modifier.weight(1f))

            CallControlBar(
                client = client,
                enabled = currentSession.state == CallState.Active,
                showSpeaker = true,
                showMic = true,
                showMessage = false,
            )

            Spacer(modifier = Modifier.height(appearance.metrics.controlToActionsSpacing))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = appearance.metrics.actionRowHorizontalPadding),
                horizontalArrangement = if (showAcceptButton) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.Center
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeclineCallButton(client = client)
                if (showAcceptButton) {
                    AcceptCallButton(client = client)
                }
            }

            Spacer(modifier = Modifier.height(appearance.metrics.bottomSpacing))
        }
    }
}
