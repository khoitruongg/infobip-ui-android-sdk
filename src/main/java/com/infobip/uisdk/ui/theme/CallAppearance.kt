package com.infobip.uisdk.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk

/**
 * Full declarative appearance for built-in call screens and building-block components.
 *
 * Hosts may override any subset; unspecified fields keep [Default] values.
 */
@Immutable
data class CallAppearance(
    val colors: CallColors = CallColors(),
    val fonts: CallFonts = CallFonts(),
    val metrics: CallMetrics = CallMetrics(),
    val icons: CallIcons = CallIcons(),
    /**
     * Background gradient stops for call screens.
     * Pass `null` to disable the gradient and use [CallColors.surface] instead.
     */
    val gradientColors: List<Color>? = listOf(
        CallGradientStart,
        CallGradientMid,
        CallGradientEnd,
    ),
) {
    companion object {
        val Default = CallAppearance()
    }
}

@Immutable
data class CallColors(
    val primary: Color = CallTeal,
    val accent: Color = CallTeal,
    val accept: Color = CallAcceptGreen,
    val decline: Color = CallDeclineRed,
    val controlOn: Color = CallTeal,
    val activeStatus: Color = CallAcceptGreen,
    val controlActiveBackground: Color = CallTeal,
    val controlInactiveBackground: Color = CallTealLight,
    val textPrimary: Color = CallTextPrimary,
    val textSecondary: Color = CallTextSecondary,
    val surface: Color = CallGradientStart,
    val iconOnControl: Color = Color.White,
    val iconInactive: Color = Color(0xFF3A3F45),
    val controlDisabled: Color = CallControlInactive,
    val avatarPlaceholderBackground: Color = CallTealLight,
    val avatarPlaceholderTint: Color = CallTeal,
    val micActiveBackground: Color = CallDeclineRed,
    /** Text color when local or remote network quality is strong. */
    val signalStrong: Color = CallSignalStrong,
    /** Text color when local or remote network quality is weak. */
    val signalWeak: Color = CallSignalWeak,
)

@Immutable
data class CallFonts(
    val title: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default,
        color = CallTextPrimary,
    ),
    val name: TextStyle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default,
        color = CallTextPrimary,
    ),
    val status: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default,
        color = CallTextSecondary,
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default,
        color = CallTextSecondary,
    ),
    val button: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default,
        color = CallTextSecondary,
    ),
)

@Immutable
data class CallMetrics(
    val avatarSize: Dp = 120.dp,
    val avatarCornerRadius: Dp = 60.dp,
    val actionButtonDiameter: Dp = 72.dp,
    val controlIconDiameter: Dp = 52.dp,
    val controlItemWidth: Dp = 88.dp,
    val controlSpacing: Dp = 8.dp,
    val horizontalPadding: Dp = 24.dp,
    val actionRowHorizontalPadding: Dp = 32.dp,
    val bottomSpacing: Dp = 48.dp,
    val controlToActionsSpacing: Dp = 40.dp,
)

/**
 * Icons used by built-in call controls.
 * Prefer template-tinted [ImageVector]s so host colors apply consistently.
 */
@Immutable
data class CallIcons(
    val accept: ImageVector = Icons.Filled.Call,
    val decline: ImageVector = Icons.Filled.Close,
    val hangUp: ImageVector = Icons.Filled.CallEnd,
    val mute: ImageVector = Icons.Filled.Mic,
    val unmute: ImageVector = Icons.Filled.MicOff,
    val speaker: ImageVector = Icons.AutoMirrored.Filled.VolumeUp,
    val earpiece: ImageVector = Icons.Filled.PhoneInTalk,
    val bluetooth: ImageVector = Icons.Filled.Bluetooth,
    val headphones: ImageVector = Icons.Filled.Headphones,
    val keypad: ImageVector = Icons.Filled.Dialpad,
    val message: ImageVector = Icons.AutoMirrored.Filled.Chat,
    val avatarPlaceholder: ImageVector = Icons.Filled.Person,
)

/**
 * Thin back-compat wrapper around [CallAppearance].
 *
 * Existing hosts that only customize the legacy color set can keep passing [CallUiTheme].
 */
data class CallUiTheme(
    val colors: CallColors = CallColors(),
) {
    fun toAppearance(): CallAppearance = CallAppearance(colors = colors)

    companion object {
        val Default = CallUiTheme()

        fun from(appearance: CallAppearance): CallUiTheme =
            CallUiTheme(colors = appearance.colors)
    }
}
