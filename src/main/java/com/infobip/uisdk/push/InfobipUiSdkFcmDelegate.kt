package com.infobip.uisdk.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import com.infobip.uisdk.internal.IncomingCallsEnabledStore
import com.infobip.uisdk.internal.InfobipCallSdkRegistry
import com.infobip.uisdk.internal.push.IncomingCallNotifier
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.sdk.impl.push.PushRegistrationService

/**
 * Delegate entry points for apps that already have their own FirebaseMessagingService.
 *
 * If the host app keeps a single service with action = com.google.firebase.MESSAGING_EVENT,
 * call these methods from that service to forward Infobip call push notifications to the UI SDK.
 */
public object InfobipUiSdkFcmDelegate {

    @JvmStatic
    public fun onMessageReceived(context: Context, remoteMessage: RemoteMessage) {
        val payload = remoteMessage.data
        if (payload.isEmpty()) return
        onDataMessage(context, payload)
    }

    @JvmStatic
    public fun onDataMessage(context: Context, payload: Map<String, String>) {
        if (payload.isEmpty()) return
        if (!InfobipRTC.getInstance().isIncomingCall(payload)) return

        // Ignore stale pushes after clearSubscriber() / force logout on this device.
        if (!IncomingCallsEnabledStore.isEnabled(context)) {
            Log.i(TAG, "Ignoring incoming-call push: subscriber cleared on this device")
            return
        }

        val client = InfobipCallSdkRegistry.client ?: return
        client.handlePushNotification(payload)

        val callerName = payload["displayName"]
            ?.takeIf { it.isNotBlank() }
            ?: payload["source"].orEmpty().ifBlank { "Unknown caller" }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            IncomingCallNotifier.show(context.applicationContext, callerName)
        }
    }

    @JvmStatic
    public fun onNewToken(context: Context, token: String) {
        PushRegistrationService.updatePushRegistration(context.applicationContext, token)
    }

    private const val TAG = "InfobipUiSdkFcm"
}
