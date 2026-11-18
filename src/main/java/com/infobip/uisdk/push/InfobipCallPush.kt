package com.infobip.uisdk.push

import com.infobip.uisdk.InfobipCallClient
import com.infobip.webrtc.sdk.api.InfobipRTC

object InfobipCallPush {

    fun isIncomingCall(payload: Map<String, String>): Boolean {
        return InfobipRTC.getInstance().isIncomingCall(payload)
    }

    fun handleMessage(
        client: InfobipCallClient,
        payload: Map<String, String>,
    ): Boolean = client.handlePushNotification(payload)

    fun dismissIncomingCallNotification(context: android.content.Context) {
        com.infobip.uisdk.internal.push.IncomingCallNotifier.dismiss(context)
    }

    fun cancelIncomingCallNotification(context: android.content.Context) {
        com.infobip.uisdk.internal.push.IncomingCallNotifier.cancelNotification(context)
    }
}

object InfobipCallIntents {
    const val ACTION_ACCEPT = "com.infobip.uisdk.action.ACCEPT_INCOMING_CALL"
    const val ACTION_DECLINE = "com.infobip.uisdk.action.DECLINE_INCOMING_CALL"
    const val EXTRA_INCOMING_CALL = "com.infobip.uisdk.extra.INCOMING_CALL"
    const val EXTRA_OPEN_INCOMING_CALL = "com.infobip.uisdk.extra.OPEN_INCOMING_CALL"
}
