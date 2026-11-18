package com.infobip.uisdk.model

/**
 * Local or remote call network quality from Infobip RTC
 * ([com.infobip.webrtc.sdk.api.model.network.NetworkQuality]).
 *
 * Exposed on [CallSession.localNetworkQuality] / [CallSession.remoteNetworkQuality].
 *
 * - For **default** strong/weak labels, use
 *   [com.infobip.uisdk.ui.components.CallSignalStrength] (or override
 *   [com.infobip.uisdk.ui.theme.CallStrings.strongSignal] /
 *   [com.infobip.uisdk.ui.theme.CallStrings.weakSignal]).
 * - For **custom text per level**, map this enum in your own UI
 *   (`Excellent` … `Bad`).
 *
 * Score ranges from [SCORE_MIN] (worst) to [SCORE_MAX] (best). Map [score] to UI bars
 * with [CallNetworkQuality] via [fromScore], or use the enum levels directly.
 */
enum class CallNetworkQuality(val score: Int) {
    Bad(1),
    Poor(2),
    Fair(3),
    Good(4),
    Excellent(5),
    ;

    /** `true` for [Bad] / [Poor] — use weak signal styling. */
    val isWeak: Boolean get() = score <= Poor.score

    companion object {
        const val SCORE_MIN = 1
        const val SCORE_MAX = 5

        fun fromScore(score: Int): CallNetworkQuality = when (score.coerceIn(SCORE_MIN, SCORE_MAX)) {
            1 -> Bad
            2 -> Poor
            3 -> Fair
            4 -> Good
            else -> Excellent
        }
    }
}
