package com.infobip.uisdk.example

import android.app.Application
import android.net.Uri
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.InfobipCallConfig
import com.infobip.uisdk.InfobipCallSdk
import com.infobip.uisdk.InfobipTokenProvider

class ExampleApplication : Application() {
    lateinit var callClient: InfobipCallClient
        private set

    /**
     * Flip this to compare token strategies:
     * - `true` — SDK refreshes tokens via [InfobipTokenProvider] when the cache is cleared
     * - `false` — omit [InfobipCallConfig.tokenProvider]; rely only on the token from
     *   [InfobipCallClient.registerSubscriber] / [InfobipCallClient.updateToken]
     *
     * Use `false` to verify that outgoing-call retries no longer fail with
     * "No cached token. Call registerSubscriber() with a token first."
     */
    val useTokenProvider: Boolean get() = USE_TOKEN_PROVIDER

    override fun onCreate() {
        super.onCreate()
        callClient = InfobipCallSdk.initialize(
            application = this,
            config = InfobipCallConfig(
                pushConfigId = BuildConfig.INFOBIP_PUSH_CONFIG_ID,
                incomingCallActivityClass = MainActivity::class.java,
                tokenProvider = if (USE_TOKEN_PROVIDER) {
                    InfobipTokenProvider { identity ->
                        val displayName = TokenStore.displayName.ifBlank { identity }
                        InfobipTokenClient.fetchToken(identity, displayName).also { token ->
                            TokenStore.token = token
                        }
                    }
                } else {
                    null
                },
                incomingCallRingtoneUri = Uri.parse(
                    "android.resource://$packageName/${R.raw.incoming_call_ringtone}",
                ),
                outgoingCallWaitingRingtoneUri = Uri.parse(
                    "android.resource://$packageName/${R.raw.outgoing_waiting_ringtone}",
                ),
            ),
        )
    }

    companion object {
        private const val USE_TOKEN_PROVIDER = false
    }
}
