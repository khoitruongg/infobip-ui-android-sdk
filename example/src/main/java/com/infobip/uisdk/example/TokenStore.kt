package com.infobip.uisdk.example

object TokenStore {
    @Volatile
    var token: String = ""

    @Volatile
    var displayName: String = ""

    fun requireToken(): String =
        token.takeIf { it.isNotBlank() }
            ?: error("WebRTC token is missing. Register for incoming calls first.")

    fun clear() {
        token = ""
        displayName = ""
    }
}
