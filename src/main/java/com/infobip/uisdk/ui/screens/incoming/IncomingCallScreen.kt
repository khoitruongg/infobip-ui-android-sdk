package com.infobip.uisdk.ui.screens.incoming

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
import com.infobip.uisdk.ui.components.AcceptCallButton
import com.infobip.uisdk.ui.components.CallBackground
import com.infobip.uisdk.ui.components.CallAvatar
import com.infobip.uisdk.ui.components.CallControlBar
import com.infobip.uisdk.ui.components.CallStatus
import com.infobip.uisdk.ui.components.DeclineCallButton
import com.infobip.uisdk.model.CallStatusUiModel
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings

@Composable
fun IncomingCallScreen(
    client: InfobipCallClient,
    onCallEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: IncomingCallViewModel = viewModel(
        factory = IncomingCallViewModel.Factory(client),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.callState) {
        if (uiState.callState == CallState.Ended) {
            onCallEnded()
        }
    }

    IncomingCallContent(
        uiState = uiState,
        client = client,
        onAccept = viewModel::onAccept,
        onDecline = viewModel::onDecline,
        modifier = modifier,
    )
}

@Composable
private fun IncomingCallContent(
    uiState: IncomingCallUiState,
    client: InfobipCallClient,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val displayStatus = when (uiState.callState) {
        CallState.Active -> uiState.statusText
        else -> strings.statusFor(uiState.callState, incoming = true)
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
            Spacer(modifier = Modifier.height(32.dp))
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
                showMessage = false,
            )

            Spacer(modifier = Modifier.height(appearance.metrics.controlToActionsSpacing))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = appearance.metrics.actionRowHorizontalPadding),
                horizontalArrangement = if (uiState.showAcceptButton) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.Center
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeclineCallButton(
                    onClick = onDecline,
                    useCloseIcon = uiState.showAcceptButton,
                    contentDescription = if (uiState.showAcceptButton) {
                        strings.declineCallContentDescription
                    } else {
                        strings.endCallContentDescription
                    },
                )
                if (uiState.showAcceptButton) {
                    AcceptCallButton(onClick = onAccept)
                }
            }

            Spacer(modifier = Modifier.height(appearance.metrics.bottomSpacing))
        }
    }
}
