package com.infobip.uisdk.internal

import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.model.AudioRoute
import com.infobip.uisdk.model.CallSession
import kotlinx.coroutines.flow.StateFlow

internal class InfobipCallClientImpl(
    private val repository: InfobipRtcCallRepository,
) : InfobipCallClient {

    override val activeSession: StateFlow<CallSession?> = repository.activeSession

    override fun callDurationSeconds(): Int = repository.callDurationSeconds()

    override suspend fun registerSubscriber(
        identity: String,
        displayName: String,
        token: String,
    ) {
        repository.registerSubscriber(identity, displayName, token)
    }

    override fun updateToken(token: String) {
        repository.updateToken(token)
    }

    override fun clearSubscriber() {
        repository.clearSubscriber()
    }

    override suspend fun registerForIncomingCalls(pushConfigId: String?) {
        repository.registerForIncomingCalls(pushConfigId)
    }

    override fun handlePushNotification(payload: Map<String, String>): Boolean {
        return repository.handleIncomingPushNotification(payload)
    }

    override suspend fun startOutgoingCall(
        destinationIdentity: String,
        customData: Map<String, String>,
        destinationDisplayName: String?,
    ): Result<CallSession> {
        return repository.startOutgoingCall(destinationIdentity, customData, destinationDisplayName)
    }

    override suspend fun retryLastCall(): Result<CallSession> {
        return repository.retryLastCall()
    }

    override suspend fun acceptIncomingCall() {
        repository.acceptIncomingCall()
    }

    override suspend fun declineIncomingCall() {
        repository.declineIncomingCall()
    }

    override suspend fun hangUp() {
        repository.hangUp()
    }

    override suspend fun setMuted(muted: Boolean) {
        repository.setMuted(muted)
    }

    override suspend fun setSpeakerOn(speakerOn: Boolean) {
        repository.setSpeakerOn(speakerOn)
    }

    override fun audioRoutes(): List<AudioRoute> = repository.audioRoutes()

    override fun activeAudioRoute(): AudioRoute? = repository.activeAudioRoute()

    override suspend fun selectAudioRoute(id: String) {
        repository.selectAudioRoute(id)
    }

    override suspend fun sendDtmf(digit: String) {
        repository.sendDtmf(digit)
    }
}
