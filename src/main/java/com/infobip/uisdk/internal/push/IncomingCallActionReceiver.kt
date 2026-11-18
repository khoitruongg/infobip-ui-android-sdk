package com.infobip.uisdk.internal.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.infobip.uisdk.internal.InfobipCallSdkRegistry
import com.infobip.uisdk.push.InfobipCallIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class IncomingCallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val client = InfobipCallSdkRegistry.client ?: return

        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                when (action) {
                    InfobipCallIntents.ACTION_ACCEPT -> {
                        client.acceptIncomingCall()
                        IncomingCallNotifier.dismiss(context)
                    }
                    InfobipCallIntents.ACTION_DECLINE -> {
                        client.declineIncomingCall()
                        IncomingCallNotifier.dismiss(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
