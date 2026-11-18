package com.infobip.uisdk.internal

import android.content.Context

/**
 * Persists whether this device should accept incoming-call push / UI.
 *
 * Survives process death so cold-start FCM still works after a successful
 * [InfobipRtcCallRepository.registerForIncomingCalls], and is cleared by
 * [InfobipRtcCallRepository.clearSubscriber] so a force-logged-out device
 * ignores stale IncomingCall pushes.
 */
internal object IncomingCallsEnabledStore {

    private const val PREFS_NAME = "infobip_uisdk_incoming_calls"
    private const val KEY_ENABLED = "incoming_calls_enabled"

    fun isEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
