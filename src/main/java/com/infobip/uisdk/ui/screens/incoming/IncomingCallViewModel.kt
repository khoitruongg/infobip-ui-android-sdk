package com.infobip.uisdk.ui.screens.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.internal.callDurationTicker
import com.infobip.uisdk.internal.formatCallDuration
import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class IncomingCallViewModel(
    private val client: InfobipCallClient,
) : ViewModel() {

    private val initialSession = client.activeSession.value

    private val _uiState = MutableStateFlow(
        IncomingCallUiState(
            remoteParticipant = initialSession?.remoteParticipant
                ?: CallParticipant(identity = "", displayName = ""),
            callType = initialSession?.type ?: CallType.FreeCall,
            callState = initialSession?.state ?: CallState.Incoming,
            statusText = when (initialSession?.state) {
                CallState.Active -> "In call"
                CallState.Ended -> "Call ended"
                else -> "Calling you"
            },
        ),
    )
    val uiState: StateFlow<IncomingCallUiState> = _uiState.asStateFlow()

    init {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            combine(
                client.activeSession,
                callDurationTicker(),
            ) { session, _ ->
                session
            }.collect { session ->
                if (session == null) {
                    _uiState.update { it.copy(callState = CallState.Ended) }
                    return@collect
                }

                val computedStatusText = if (session.state == CallState.Active) {
                    formatCallDuration(client.callDurationSeconds())
                } else {
                    when (session.state) {
                        CallState.Ended -> "Call ended"
                        else -> "Calling you"
                    }
                }

                _uiState.update {
                    it.copy(
                        callType = session.type,
                        remoteParticipant = session.remoteParticipant,
                        isMuted = session.isMuted,
                        isSpeakerOn = session.isSpeakerOn,
                        callState = session.state,
                        statusText = computedStatusText,
                    )
                }
            }
        }
    }

    fun onAccept() {
        _uiState.update {
            it.copy(
                callState = CallState.Active,
                statusText = formatCallDuration(client.callDurationSeconds()),
            )
        }
        viewModelScope.launch {
            client.acceptIncomingCall()
        }
    }

    fun onDecline() {
        viewModelScope.launch {
            if (_uiState.value.callState == CallState.Active) {
                client.hangUp()
            } else {
                client.declineIncomingCall()
            }
        }
    }

    fun onToggleMute() {
        viewModelScope.launch {
            val muted = !_uiState.value.isMuted
            client.setMuted(muted)
        }
    }

    fun onToggleSpeaker() {
        viewModelScope.launch {
            val speakerOn = !_uiState.value.isSpeakerOn
            client.setSpeakerOn(speakerOn)
        }
    }

    class Factory(
        private val client: InfobipCallClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IncomingCallViewModel(client) as T
        }
    }
}
