package com.infobip.uisdk.internal

internal class SubscriberIdentityStore {
    private var identity: String? = null
    private var displayName: String? = null

    fun getIdentity(): String? = identity?.takeIf { it.isNotBlank() }

    fun getDisplayName(): String = displayName?.takeIf { it.isNotBlank() } ?: getIdentity().orEmpty()

    fun save(identity: String, displayName: String) {
        this.identity = identity.trim()
        this.displayName = displayName.trim()
    }

    fun clear() {
        identity = null
        displayName = null
    }
}
