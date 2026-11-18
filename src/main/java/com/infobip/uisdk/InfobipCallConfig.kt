package com.infobip.uisdk

import android.app.Activity
import android.net.Uri

data class InfobipCallConfig(
    /** Infobip push configuration ID for background incoming calls. */
    val pushConfigId: String,
    /**
     * Activity opened when user taps an incoming-call notification.
     * Must use [android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP] launch mode.
     */
    val incomingCallActivityClass: Class<out Activity>,
    /**
     * Optional. Used when the cached token expires and the SDK needs a fresh one.
     */
    val tokenProvider: InfobipTokenProvider? = null,
    /**
     * Optional IncomingCall ringtone. When null, the device default ringtone is used.
     *
     * For a raw resource in the host app:
     * `Uri.parse("android.resource://${packageName}/${R.raw.my_ringtone}")`
     */
    val incomingCallRingtoneUri: Uri? = null,
    /**
     * Optional outgoing waiting / ringback tone while the callee has not answered.
     * When null, the SDK default asset (`R.raw.outgoing_waiting_ringtone`) is used.
     *
     * For a raw resource in the host app:
     * `Uri.parse("android.resource://${packageName}/${R.raw.my_ringback}")`
     */
    val outgoingCallWaitingRingtoneUri: Uri? = null,
    /**
     * When `true` (default), posts the incoming-call notification even if the app is in
     * the foreground. Set to `false` to show the notification only when the app is
     * backgrounded or killed (in-app UI still receives [com.infobip.uisdk.model.CallState.Incoming]).
     */
    val showIncomingCallNotificationInForeground: Boolean = true,
)
