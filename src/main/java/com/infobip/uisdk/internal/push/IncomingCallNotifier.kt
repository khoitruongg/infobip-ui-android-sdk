package com.infobip.uisdk.internal.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.infobip.uisdk.R
import com.infobip.uisdk.internal.InfobipCallSdkRegistry
import com.infobip.uisdk.push.InfobipCallIntents

internal object IncomingCallNotifier {

    // Bumped when channel sound/vibration defaults change (Android ignores updates to existing channels).
    private const val CHANNEL_ID = "infobip_uisdk_incoming_calls_v2"
    private const val NOTIFICATION_ID = 42_001

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(context: Context, callerDisplayName: String) {
        val appContext = context.applicationContext
        IncomingCallRingtonePlayer.start(appContext)

        val config = InfobipCallSdkRegistry.config ?: return
        val isAppInForeground =
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (isAppInForeground && !config.showIncomingCallNotificationInForeground) {
            return
        }
        if (!canPostNotifications(context)) {
            return
        }

        val activityClass = config.incomingCallActivityClass

        createChannel(appContext)

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val openIntent = Intent(appContext, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(InfobipCallIntents.EXTRA_INCOMING_CALL, true)
        }
        val contentIntent = PendingIntent.getActivity(appContext, 0, openIntent, pendingIntentFlags)
        val fullScreenIntent = PendingIntent.getActivity(appContext, 1, openIntent, pendingIntentFlags)

        val acceptIntent = Intent(appContext, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            action = InfobipCallIntents.ACTION_ACCEPT
        }
        val declineIntent = Intent(appContext, IncomingCallActionReceiver::class.java).apply {
            action = InfobipCallIntents.ACTION_DECLINE
        }
        val acceptPendingIntent = PendingIntent.getActivity(appContext, 2, acceptIntent, pendingIntentFlags)
        val declinePendingIntent = PendingIntent.getBroadcast(appContext, 3, declineIntent, pendingIntentFlags)

        val customNotificationView = buildIncomingCallRemoteViews(
            appContext,
            callerDisplayName,
            acceptPendingIntent,
            declinePendingIntent,
        )

        val notificationBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_blank)
            .setContentTitle(callerDisplayName)
            .setCustomHeadsUpContentView(customNotificationView)
            .setCustomContentView(customNotificationView)
            .setCustomBigContentView(customNotificationView)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }

    fun dismiss(context: Context) {
        IncomingCallRingtonePlayer.stop()
        cancelNotification(context)
    }

    private fun buildIncomingCallRemoteViews(
        context: Context,
        callerDisplayName: String,
        acceptPendingIntent: PendingIntent,
        declinePendingIntent: PendingIntent,
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_incoming_call_heads_up)
        remoteViews.setTextViewText(R.id.notification_caller_name, callerDisplayName)
        remoteViews.setOnClickPendingIntent(R.id.notification_decline_button, declinePendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.notification_accept_button, acceptPendingIntent)
        return remoteViews
    }

    private fun canPostNotifications(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.incoming_call_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.incoming_call_channel_description)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            // Sound/vibration are owned by IncomingCallRingtonePlayer for looping control.
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
