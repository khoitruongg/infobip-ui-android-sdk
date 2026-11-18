package com.infobip.uisdk.ui.screens.outgoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.internal.callDurationTicker
import com.infobip.uisdk.internal.formatCallDuration
import com.infobip.uisdk.internal.toOutgoingStatusText
import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.ui.theme.CallStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OutgoingCallViewModel(
    private val client: InfobipCallClient,
    private val destinationIdentity: String,
    private val customData: Map<String, String>,
    private val destinationDisplayName: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OutgoingCallUiState(
            remoteParticipant = CallParticipant.fromIdentity(
                identity = destinationIdentity,
                fallbackDisplayName = destinationDisplayName,
            ),
        ),
    )
    val uiState: StateFlow<OutgoingCallUiState> = _uiState.asStateFlow()

    private var wasConnected = false
    private var hadActiveSession = false

    init {
        observeActiveSession()
        startCall()
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
                    if (hadActiveSession) {
                        val currentState = _uiState.value.callState
                        if (currentState != CallState.Ended && currentState != CallState.Failed) {
                            val terminalState = if (wasConnected) CallState.Ended else CallState.Failed
                            _uiState.update {
                                it.copy(
                                    callState = terminalState,
                                    statusText = terminalState.toOutgoingStatusText(wasConnected),
                                )
                            }
                        }
                    }
                    return@collect
                }

                hadActiveSession = true

                if (session.state == CallState.Active) {
                    wasConnected = true
                }

                val computedStatusText = if (session.state == CallState.Active) {
                    formatCallDuration(client.callDurationSeconds())
                } else if (!wasConnected && (session.state == CallState.Ended || session.state == CallState.Failed)) {
                    session.failureReason?.takeIf { it.isNotBlank() }
                        ?: session.state.toOutgoingStatusText(wasConnected = false)
                } else {
                    session.failureReason?.takeIf { it.isNotBlank() }
                        ?: session.state.toOutgoingStatusText(wasConnected)
                }

                val remoteParticipant = destinationDisplayName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { session.remoteParticipant.copy(displayName = it) }
                    ?: session.remoteParticipant

                _uiState.update {
                    it.copy(
                        callType = session.type,
                        statusText = computedStatusText,
                        remoteParticipant = remoteParticipant,
                        isMuted = session.isMuted,
                        isSpeakerOn = session.isSpeakerOn,
                        callState = session.state,
                    )
                }
            }
        }
    }

    private fun startCall() {
        viewModelScope.launch {
            client.startOutgoingCall(
                destinationIdentity = destinationIdentity,
                customData = customData,
                destinationDisplayName = destinationDisplayName,
            )
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            callState = CallState.Failed,
                            statusText = error.message ?: CallStrings.Default.callFailed,
                        )
                    }
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

    fun onHangUp() {
        viewModelScope.launch {
            client.hangUp()
        }
    }

    class Factory(
        private val client: InfobipCallClient,
        private val destinationIdentity: String,
        private val customData: Map<String, String>,
        private val destinationDisplayName: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OutgoingCallViewModel(
                client,
                destinationIdentity,
                customData,
                destinationDisplayName,
            ) as T
        }
    }
}
