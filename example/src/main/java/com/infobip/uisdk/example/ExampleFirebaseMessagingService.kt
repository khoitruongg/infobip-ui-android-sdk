package com.infobip.uisdk.example

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.infobip.uisdk.push.InfobipUiSdkFcmDelegate

/**
 * Example of an app-owned FirebaseMessagingService.
 *
 * Use this pattern when your app must keep its own service with
 * action = com.google.firebase.MESSAGING_EVENT, and forward Infobip call pushes to the UI SDK.
 */
class ExampleFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // App-specific handling can live here (analytics, other providers, etc.)
        InfobipUiSdkFcmDelegate.onMessageReceived(this, remoteMessage)
    }

    override fun onNewToken(token: String) {
        // App-specific handling can live here (send token to your backend, etc.)
        InfobipUiSdkFcmDelegate.onNewToken(this, token)
    }
}

