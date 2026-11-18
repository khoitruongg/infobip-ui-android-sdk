package com.infobip.uisdk

import android.app.Application
import androidx.compose.runtime.Composable
import com.infobip.uisdk.internal.InfobipCallClientImpl
import com.infobip.uisdk.internal.InfobipCallSdkRegistry
import com.infobip.uisdk.internal.InfobipRtcCallRepository
import com.infobip.uisdk.internal.SubscriberIdentityStore
import com.infobip.uisdk.ui.theme.CallAppearance
import com.infobip.uisdk.ui.theme.CallStrings
import com.infobip.uisdk.ui.theme.CallUiTheme
import com.infobip.uisdk.ui.theme.InfobipCallTheme

object InfobipCallSdk {

    fun initialize(
        application: Application,
        config: InfobipCallConfig,
    ): InfobipCallClient {
        val subscriberIdentityStore = SubscriberIdentityStore()
        val repository = InfobipRtcCallRepository(
            appContext = application.applicationContext,
            config = config,
            subscriberIdentityStore = subscriberIdentityStore,
        )
        val client = InfobipCallClientImpl(repository)
        InfobipCallSdkRegistry.config = config
        InfobipCallSdkRegistry.client = client
        return client
    }

    /**
     * Applies [appearance] and [strings] to all SDK call composables in [content].
     *
     * Prefer this overload for full branding / localization.
     */
    @Composable
    fun Theme(
        appearance: CallAppearance = CallAppearance.Default,
        strings: CallStrings = CallStrings.Default,
        content: @Composable () -> Unit,
    ) {
        InfobipCallTheme(
            appearance = appearance,
            strings = strings,
            content = content,
        )
    }

    /**
     * Back-compat overload that only customizes the legacy color set.
     */
    @Composable
    fun Theme(
        theme: CallUiTheme,
        content: @Composable () -> Unit,
    ) {
        InfobipCallTheme(
            appearance = theme.toAppearance(),
            content = content,
        )
    }
}
