package com.infobip.uisdk.ui.screens.outgoing

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallStatusUiModel
import com.infobip.uisdk.ui.components.CallAvatar
import com.infobip.uisdk.ui.components.CallBackground
import com.infobip.uisdk.ui.components.CallControlBar
import com.infobip.uisdk.ui.components.CallStatus
import com.infobip.uisdk.ui.components.DeclineCallButton
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings
import kotlinx.coroutines.delay

/**
 * Default outgoing-call UI.
 *
 * [destinationDisplayName] is shown as the callee label and forwarded to
 * [InfobipCallClient.startOutgoingCall] (falls back to [destinationIdentity] when null/blank).
 */
@Composable
fun OutgoingCallScreen(
    client: InfobipCallClient,
    destinationIdentity: String,
    destinationDisplayName: String? = null,
    customData: Map<String, String> = emptyMap(),
    onCallEnded: () -> Unit,
    onCallFailed: () -> Unit = onCallEnded,
    modifier: Modifier = Modifier,
) {
    val viewModel: OutgoingCallViewModel = viewModel(
        factory = OutgoingCallViewModel.Factory(
            client,
            destinationIdentity,
            customData,
            destinationDisplayName,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Ringback is started/stopped by InfobipRtcCallRepository so custom outgoing UIs hear it too.

    LaunchedEffect(uiState.callState) {
        when (uiState.callState) {
            CallState.Ended -> {
                delay(TERMINAL_CALL_DISMISS_DELAY_MS)
                onCallEnded()
            }
            CallState.Failed -> {
                delay(TERMINAL_CALL_DISMISS_DELAY_MS)
                onCallFailed()
            }
            else -> Unit
        }
    }

    OutgoingCallContent(
        uiState = uiState,
        client = client,
        onHangUp = viewModel::onHangUp,
        modifier = modifier,
    )
}

@Composable
private fun OutgoingCallContent(
    uiState: OutgoingCallUiState,
    client: InfobipCallClient,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val displayStatus = when (uiState.callState) {
        CallState.Active -> uiState.statusText
        CallState.Failed -> uiState.statusText.takeIf { it.isNotBlank() && it != strings.callFailed }
            ?: strings.outgoingStatusFor(uiState.callState, wasConnected = false)
        else -> strings.outgoingStatusFor(uiState.callState)
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
            Spacer(modifier = Modifier.height(appearance.metrics.controlToActionsSpacing - appearance.metrics.controlSpacing))
            CallStatus(
                status = CallStatusUiModel(
                    callTypeLabel = strings.freeCallTitle,
                    callState = uiState.callState,
                    statusText = displayStatus,
                ),
            )

            Spacer(modifier = Modifier.weight(1f))

            CallAvatar(participant = uiState.remoteParticipant)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = uiState.remoteParticipant.displayName,
                style = appearance.fonts.name,
                color = appearance.colors.textPrimary,
            )

            Spacer(modifier = Modifier.weight(1f))

            CallControlBar(
                client = client,
                enabled = uiState.callControlsEnabled,
                showSpeaker = true,
                showMic = true,
                showMessage = true,
                onMessageClick = { /* host can intercept via wrapper screen if needed */ },
            )

            Spacer(modifier = Modifier.height(appearance.metrics.controlToActionsSpacing))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = appearance.metrics.actionRowHorizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeclineCallButton(
                    onClick = onHangUp,
                    useCloseIcon = false,
                    contentDescription = strings.endCallContentDescription,
                )
            }

            Spacer(modifier = Modifier.height(appearance.metrics.bottomSpacing))
        }
    }
}

private const val TERMINAL_CALL_DISMISS_DELAY_MS = 2_000L
