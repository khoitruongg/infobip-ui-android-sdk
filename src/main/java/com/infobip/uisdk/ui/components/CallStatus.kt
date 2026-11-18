package com.infobip.uisdk.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.internal.callDurationTicker
import com.infobip.uisdk.internal.formatCallDuration
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallStatusUiModel
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings
import kotlinx.coroutines.flow.combine

@Composable
fun CallStatus(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    titleStyle: TextStyle? = null,
    statusStyle: TextStyle? = null,
    titleColor: Color? = null,
    statusColor: Color? = null,
    activeStatusColor: Color? = null,
) {
    val strings = currentCallStrings
    val session by client.activeSession.collectAsStateWithLifecycle()
    val tick by callDurationTicker().collectAsStateWithLifecycle(initialValue = 0L)
    val durationSeconds = remember(session?.state, tick) { client.callDurationSeconds() }
    val resolvedStatus = statusText ?: session?.let {
        resolveStatusText(it.state, durationSeconds, strings)
    } ?: ""
    CallStatus(
        status = CallStatusUiModel(
            callTypeLabel = session?.type?.label ?: strings.freeCallTitle,
            callState = session?.state ?: CallState.Idle,
            statusText = resolvedStatus,
        ),
        modifier = modifier,
        titleStyle = titleStyle,
        statusStyle = statusStyle,
        titleColor = titleColor,
        statusColor = statusColor,
        activeStatusColor = activeStatusColor,
    )
}

@Composable
fun CallStatus(
    status: CallStatusUiModel,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle? = null,
    statusStyle: TextStyle? = null,
    titleColor: Color? = null,
    statusColor: Color? = null,
    activeStatusColor: Color? = null,
) {
    val appearance = currentCallAppearance
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (status.callTypeLabel.isNotBlank()) {
            Text(
                text = status.callTypeLabel,
                style = titleStyle ?: appearance.fonts.title,
                color = titleColor ?: appearance.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = status.statusText,
            style = statusStyle ?: appearance.fonts.status,
            color = if (status.callState == CallState.Active) {
                activeStatusColor ?: appearance.colors.activeStatus
            } else {
                statusColor ?: appearance.colors.textSecondary
            },
        )
    }
}

@Composable
internal fun rememberCallStatusText(client: InfobipCallClient): String {
    val strings = currentCallStrings
    val session by client.activeSession.collectAsStateWithLifecycle()
    val tick by callDurationTicker().collectAsStateWithLifecycle(initialValue = 0L)
    val durationSeconds = remember(session?.state, tick) { client.callDurationSeconds() }
    return session?.let { resolveStatusText(it.state, durationSeconds, strings) } ?: ""
}

internal fun resolveStatusText(
    state: CallState,
    durationSeconds: Int,
    strings: com.infobip.uisdk.ui.theme.CallStrings,
): String = strings.statusFor(
    state = state,
    durationText = if (state == CallState.Active) formatCallDuration(durationSeconds) else null,
)

internal fun observeLiveStatusText(
    client: InfobipCallClient,
    strings: com.infobip.uisdk.ui.theme.CallStrings = com.infobip.uisdk.ui.theme.CallStrings.Default,
): kotlinx.coroutines.flow.Flow<String> {
    return combine(client.activeSession, callDurationTicker()) { session, _ ->
        if (session == null) return@combine ""
        if (session.state == CallState.Active) {
            formatCallDuration(client.callDurationSeconds())
        } else {
            strings.statusFor(
                state = session.state,
                failureReason = session.failureReason,
                incoming = session.state == CallState.Incoming,
            )
        }
    }
}
