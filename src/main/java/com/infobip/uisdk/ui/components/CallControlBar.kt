package com.infobip.uisdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import com.infobip.uisdk.InfobipCallClient

/**
 * Per-control visual overrides for [CallControlBar].
 *
 * Unset fields fall back to [CallControlBar] shared defaults, then to [com.infobip.uisdk.ui.theme.CallAppearance].
 */
@Immutable
data class CallControlStyle(
    val icon: ImageVector? = null,
    val activeIcon: ImageVector? = null,
    val backgroundColor: Color? = null,
    val activeBackgroundColor: Color? = null,
    val iconTint: Color? = null,
    val activeIconTint: Color? = null,
    val label: String? = null,
    val activeLabel: String? = null,
    val labelStyle: TextStyle? = null,
    val labelColor: Color? = null,
)

/**
 * A reusable control strip for call screens.
 *
 * Layout-only: callers provide the controls (Speaker/Mute/Message/etc) via [content].
 * Customize each child directly (e.g. [MicButton] icon / colors / label style).
 */
@Composable
fun CallControlBar(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

/**
 * Convenience overload that renders the SDK-provided controls with consistent styling.
 *
 * Shared [labelStyle] / [labelColor] / colors apply to every visible control unless a
 * per-control [CallControlStyle] overrides them. Unset values keep [com.infobip.uisdk.ui.theme.CallAppearance].
 */
@Composable
fun CallControlBar(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSpeaker: Boolean = true,
    showMic: Boolean = true,
    showMessage: Boolean = false,
    onSpeakerPress: (() -> Unit)? = null,
    onMicPress: (() -> Unit)? = null,
    onMessageClick: (() -> Unit)? = null,
    showLabel: Boolean = true,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
    backgroundColor: Color? = null,
    activeBackgroundColor: Color? = null,
    iconTint: Color? = null,
    activeIconTint: Color? = null,
    speakerStyle: CallControlStyle = CallControlStyle(),
    micStyle: CallControlStyle = CallControlStyle(),
    messageStyle: CallControlStyle = CallControlStyle(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
) {
    CallControlBar(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
    ) {
        if (showSpeaker) {
            SpeakerButton(
                client = client,
                enabled = enabled,
                showLabel = showLabel,
                onPress = onSpeakerPress,
                icon = speakerStyle.icon,
                backgroundColor = speakerStyle.backgroundColor ?: backgroundColor,
                activeBackgroundColor = speakerStyle.activeBackgroundColor ?: activeBackgroundColor,
                iconTint = speakerStyle.iconTint ?: iconTint,
                activeIconTint = speakerStyle.activeIconTint ?: activeIconTint,
                label = speakerStyle.label,
                labelStyle = speakerStyle.labelStyle ?: labelStyle,
                labelColor = speakerStyle.labelColor ?: labelColor,
            )
        }
        if (showMic) {
            MicButton(
                client = client,
                enabled = enabled,
                showLabel = showLabel,
                onPress = onMicPress,
                icon = micStyle.icon,
                activeIcon = micStyle.activeIcon,
                backgroundColor = micStyle.backgroundColor ?: backgroundColor,
                activeBackgroundColor = micStyle.activeBackgroundColor ?: activeBackgroundColor,
                iconTint = micStyle.iconTint ?: iconTint,
                activeIconTint = micStyle.activeIconTint ?: activeIconTint,
                label = micStyle.label,
                activeLabel = micStyle.activeLabel,
                labelStyle = micStyle.labelStyle ?: labelStyle,
                labelColor = micStyle.labelColor ?: labelColor,
            )
        }
        if (showMessage && onMessageClick != null) {
            MessageButton(
                enabled = enabled,
                showLabel = showLabel,
                onClick = onMessageClick,
                icon = messageStyle.icon,
                backgroundColor = messageStyle.backgroundColor ?: backgroundColor,
                iconTint = messageStyle.iconTint ?: iconTint,
                label = messageStyle.label,
                labelStyle = messageStyle.labelStyle ?: labelStyle,
                labelColor = messageStyle.labelColor ?: labelColor,
            )
        }
    }
}
