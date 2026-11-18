package com.infobip.uisdk.ui.theme

import androidx.compose.runtime.Immutable
import com.infobip.uisdk.model.CallState

/**
 * Every user-facing string used by built-in call screens and components.
 *
 * Sensible English defaults; hosts override any subset via [InfobipCallSdk.Theme].
 */
@Immutable
data class CallStrings(
    val freeCallTitle: String = "Free call",
    val connecting: String = "Connecting...",
    val ringing: String = "Ringing...",
    val reconnecting: String = "Reconnecting...",
    val calling: String = "Calling…",
    val callingYou: String = "Incoming call",
    val inCall: String = "In call",
    val callEnded: String = "Call ended",
    val callFailed: String = "Call failed",
    val userBusy: String = "User is busy",
    val mute: String = "Mute",
    val unmute: String = "Unmute",
    val speaker: String = "Speaker",
    val bluetooth: String = "Bluetooth",
    val headphones: String = "Headphones",
    val audio: String = "Audio",
    val message: String = "Message",
    val keypad: String = "Keypad",
    val audioSourceTitle: String = "Audio source",
    val builtInReceiver: String = "Phone",
    val earpiece: String = "Earpiece",
    val externalSpeaker: String = "Speaker",
    val unreachableTitle: String = "User unreachable",
    val unreachableSubtitle: String = "The person you're calling is not available right now.",
    val tryAgain: String = "Try again",
    val sendMessage: String = "Send message",
    val close: String = "Close",
    val acceptCallContentDescription: String = "Accept call",
    val declineCallContentDescription: String = "Decline call",
    val endCallContentDescription: String = "End call",
    val micPermissionTitle: String = "Microphone permission required",
    val micPermissionMessage: String = "Allow microphone access to place and receive calls.",
    val micPermissionConfirm: String = "Open settings",
    val micPermissionDismiss: String = "Not now",
    val strongSignal: String = "Strong signal",
    val weakSignal: String = "Weak signal...",
) {
    companion object {
        val Default = CallStrings()
    }

    fun statusFor(
        state: CallState,
        durationText: String? = null,
        wasConnected: Boolean = false,
        failureReason: String? = null,
        incoming: Boolean = false,
    ): String = when (state) {
        CallState.Incoming -> callingYou
        CallState.Connecting -> if (incoming) callingYou else connecting
        CallState.Ringing -> ringing
        CallState.Active -> durationText ?: inCall
        CallState.Ended -> if (!wasConnected && !incoming) userBusy else callEnded
        CallState.Failed -> failureReason?.takeIf { it.isNotBlank() }
            ?: if (!wasConnected) userBusy else callFailed
        CallState.Idle -> ""
    }

    fun outgoingStatusFor(
        state: CallState,
        wasConnected: Boolean = false,
        failureReason: String? = null,
        durationText: String? = null,
    ): String = when {
        !wasConnected && (state == CallState.Ended || state == CallState.Failed) ->
            failureReason?.takeIf { it.isNotBlank() } ?: userBusy
        state == CallState.Connecting -> connecting
        state == CallState.Ringing -> ringing
        state == CallState.Active -> durationText ?: inCall
        state == CallState.Ended -> callEnded
        state == CallState.Failed -> failureReason?.takeIf { it.isNotBlank() } ?: callFailed
        else -> connecting
    }
}
