package com.infobip.uisdk.internal

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Formats Infobip RTC [durationSeconds] (`WebrtcCall.duration()`) as `MM:SS`. */
internal fun formatCallDuration(durationSeconds: Int): String {
    val totalSeconds = durationSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/** Emits every second so UI can re-read live call duration from Infobip RTC. */
internal fun callDurationTicker(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1000)
    }
}

internal fun com.infobip.uisdk.model.CallState.toOutgoingStatusText(wasConnected: Boolean = false): String = when {
    !wasConnected && (this == com.infobip.uisdk.model.CallState.Ended ||
        this == com.infobip.uisdk.model.CallState.Failed) -> "User is busy"
    this == com.infobip.uisdk.model.CallState.Connecting -> "Connecting..."
    this == com.infobip.uisdk.model.CallState.Ringing -> "Ringing..."
    this == com.infobip.uisdk.model.CallState.Active -> "In call"
    this == com.infobip.uisdk.model.CallState.Ended -> "Call ended"
    this == com.infobip.uisdk.model.CallState.Failed -> "Call failed"
    else -> "Connecting..."
}
