package com.infobip.uisdk.model

/**
 * Infobip RTC hangup / error status (Portunus `status` / RTC [com.infobip.webrtc.sdk.api.model.ErrorCode]).
 *
 * Match on [id] (stable) or [name] for client-specific handling, for example:
 * - [ID_BUSY] – callee busy
 * - [ID_CANCELLED] – call cancelled before connect
 * - [ID_NORMAL_HANGUP] – normal hangup (use [CallSession.endedBy] for local vs remote)
 * - [ID_REJECTED] / [ID_NO_ANSWER] – callee rejected or did not answer
 */
data class CallEndCode(
    val id: Int,
    val name: String,
    val description: String,
) {
    companion object {
        const val ID_NORMAL_HANGUP = 10_000
        const val ID_ANSWERED_ELSEWHERE = 10_001
        const val ID_MACHINE_DETECTED = 10_002
        const val ID_HUMAN_DETECTED = 10_003
        const val ID_DEVICE_FORBIDDEN = 10_100
        const val ID_DEVICE_NOT_FOUND = 10_101
        const val ID_DEVICE_UNAVAILABLE = 10_102
        const val ID_MEDIA_ERROR = 10_103
        const val ID_NO_ANSWER = 10_200
        const val ID_BUSY = 10_201
        const val ID_CANCELLED = 10_202
        const val ID_REJECTED = 10_203
        const val ID_FORBIDDEN = 10_300
        const val ID_INSUFFICIENT_FUNDS = 10_301
        const val ID_UNAUTHENTICATED = 10_302
        const val ID_DESTINATION_NOT_FOUND = 10_303
        const val ID_DESTINATION_UNAVAILABLE = 10_304
        const val ID_INVALID_DESTINATION = 10_305
        const val ID_INVALID_REQUEST = 10_306
        const val ID_REQUEST_TIMEOUT = 10_307
        const val ID_NETWORK_ERROR = 10_308
        const val ID_SERVICE_UNAVAILABLE = 10_309
        const val ID_UNKNOWN = 10_310
        const val ID_FEATURE_UNAVAILABLE = 10_311
        const val ID_URL_NOT_FOUND = 10_400
        const val ID_URL_UNREACHABLE = 10_401
        const val ID_INVALID_RESPONSE = 10_402

        fun from(
            id: Int,
            name: String?,
            description: String?,
        ): CallEndCode = CallEndCode(
            id = id,
            name = name?.takeIf { it.isNotBlank() } ?: "UNKNOWN",
            description = description?.takeIf { it.isNotBlank() } ?: "The call has ended.",
        )
    }
}

/**
 * Who initiated the terminal hangup / decline from this device's perspective.
 *
 * Infobip [CallEndCode.ID_NORMAL_HANGUP] does not say which party hung up;
 * use this with [CallSession.endCode] to distinguish caller cancel vs receiver hangup.
 */
enum class CallEndedBy {
    /** This device hung up, declined, or cancelled. */
    Local,

    /** Remote peer or platform ended the call (busy, reject, remote hangup, network, …). */
    Remote,
}
