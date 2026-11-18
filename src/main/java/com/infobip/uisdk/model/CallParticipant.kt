package com.infobip.uisdk.model

data class CallParticipant(
    val identity: String,
    val displayName: String,
    val avatarUrl: String? = null,
) {
    companion object {
        fun fromIdentity(
            identity: String,
            fallbackDisplayName: String? = null,
            avatarUrl: String? = null,
        ): CallParticipant = CallParticipant(
            identity = identity,
            displayName = fallbackDisplayName?.takeIf { it.isNotBlank() } ?: identity,
            avatarUrl = avatarUrl,
        )
    }
}
