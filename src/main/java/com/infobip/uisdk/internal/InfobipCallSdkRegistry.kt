package com.infobip.uisdk.internal

import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.InfobipCallConfig

internal object InfobipCallSdkRegistry {
    @Volatile
    var config: InfobipCallConfig? = null

    @Volatile
    var client: InfobipCallClient? = null
}
