package com.infobip.uisdk.model

/**
 * A selectable audio output route for an active call.
 */
data class AudioRoute(
    val id: String,
    val name: String,
    val type: AudioRouteType,
    val isActive: Boolean = false,
)

enum class AudioRouteType {
    Earpiece,
    Speaker,
    Bluetooth,
    Headphones,
    Other,
}

object AudioRouteIds {
    const val EARPIECE = "earpiece"
    const val SPEAKER = "speaker"
    const val BLUETOOTH = "bluetooth"
    const val HEADPHONES = "headphones"
}
