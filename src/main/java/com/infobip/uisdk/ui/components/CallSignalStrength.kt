package com.infobip.uisdk.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.model.CallNetworkQuality
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings

/**
 * Default network signal text driven by [InfobipCallClient.activeSession]
 * ("Strong signal" / "Weak signal..." via [com.infobip.uisdk.ui.theme.CallStrings]).
 *
 * Override labels with [strongText] / [weakText], or theme-wide via
 * [com.infobip.uisdk.ui.theme.CallStrings.strongSignal] /
 * [com.infobip.uisdk.ui.theme.CallStrings.weakSignal].
 *
 * Use this for the built-in strong/weak labels. For per-level copy
 * (`Excellent` … `Bad`), read [com.infobip.uisdk.model.CallSession.localNetworkQuality]
 * or [com.infobip.uisdk.model.CallSession.remoteNetworkQuality] and map
 * [CallNetworkQuality] yourself.
 *
 * Use [showRemote] for the remote participant's quality. Place in custom call screens;
 * default SDK screens do not include it.
 *
 * Renders nothing while quality is still unknown (`null`).
 */
@Composable
fun CallSignalStrength(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    showRemote: Boolean = false,
    strongText: String? = null,
    weakText: String? = null,
    strongColor: Color? = null,
    weakColor: Color? = null,
    textStyle: TextStyle? = null,
    textColor: Color? = null,
) {
    val session by client.activeSession.collectAsStateWithLifecycle()
    val quality = if (showRemote) {
        session?.remoteNetworkQuality
    } else {
        session?.localNetworkQuality
    }
    CallSignalStrength(
        quality = quality,
        modifier = modifier,
        strongText = strongText,
        weakText = weakText,
        strongColor = strongColor,
        weakColor = weakColor,
        textStyle = textStyle,
        textColor = textColor,
    )
}

/**
 * Default strong/weak network signal text for a [CallNetworkQuality] value
 * ([com.infobip.uisdk.ui.theme.CallStrings.strongSignal] /
 * [com.infobip.uisdk.ui.theme.CallStrings.weakSignal], based on [CallNetworkQuality.isWeak]).
 *
 * Pass [strongText] / [weakText] to override those strings for this instance.
 *
 * For custom text per enum level, map [CallNetworkQuality] in your UI instead of this composable.
 *
 * Pass `null` [quality] to render nothing. Hosts can also bind this to their own source.
 *
 * Colors fall back to [com.infobip.uisdk.ui.theme.CallColors.signalStrong] /
 * [com.infobip.uisdk.ui.theme.CallColors.signalWeak].
 */
@Composable
fun CallSignalStrength(
    quality: CallNetworkQuality?,
    modifier: Modifier = Modifier,
    strongText: String? = null,
    weakText: String? = null,
    strongColor: Color? = null,
    weakColor: Color? = null,
    textStyle: TextStyle? = null,
    textColor: Color? = null,
) {
    if (quality == null) return

    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val toneColor = if (quality.isWeak) {
        weakColor ?: appearance.colors.signalWeak
    } else {
        strongColor ?: appearance.colors.signalStrong
    }

    Text(
        text = if (quality.isWeak) {
            weakText ?: strings.weakSignal
        } else {
            strongText ?: strings.strongSignal
        },
        modifier = modifier,
        style = textStyle ?: appearance.fonts.status,
        color = textColor ?: toneColor,
    )
}
