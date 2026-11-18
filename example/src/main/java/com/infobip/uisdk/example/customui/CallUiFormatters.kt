package com.infobip.uisdk.example.customui

internal fun formatElapsedTime(durationSeconds: Int): String {
    val totalSeconds = durationSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
