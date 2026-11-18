package com.infobip.uisdk.model

data class CallStatusUiModel(
    val callTypeLabel: String,
    val callState: CallState,
    val statusText: String,
    val durationText: String? = null,
)
