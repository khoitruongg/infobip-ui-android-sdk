package com.infobip.uisdk

/**
 * Client fetches WebRTC tokens from their backend (which calls Infobip POST /webrtc/1/token).
 */
fun interface InfobipTokenProvider {
    suspend fun getToken(identity: String): String
}
