package com.infobip.uisdk.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.infobip.uisdk.ui.theme.CallAppearance
import com.infobip.uisdk.ui.theme.CallColors
import com.infobip.uisdk.ui.theme.CallFonts
import com.infobip.uisdk.ui.theme.CallIcons
import com.infobip.uisdk.ui.theme.CallStrings

/**
 * Sample host branding passed to [com.infobip.uisdk.InfobipCallSdk.Theme].
 *
 * Swap colors / fonts / icons / strings here to see how the SDK call UI picks them up.
 * Unset fields keep SDK defaults.
 */
object ExampleCallTheme {

    private val BrandIndigo = Color(0xFF3949AB)
    private val BrandIndigoLight = Color(0xFFC5CAE9)
    private val AcceptGreen = Color(0xFF2E7D32)
    private val DeclineRed = Color(0xFFC62828)
    private val TextPrimary = Color(0xFF1A237E)
    private val TextSecondary = Color(0xFF5C6BC0)

    val appearance = CallAppearance(
        colors = CallColors(
            primary = BrandIndigo,
            accent = BrandIndigo,
            accept = AcceptGreen,
            decline = DeclineRed,
            controlOn = BrandIndigo,
            activeStatus = AcceptGreen,
            controlActiveBackground = BrandIndigo,
            controlInactiveBackground = BrandIndigoLight,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            surface = Color(0xFFE8EAF6),
            avatarPlaceholderBackground = BrandIndigoLight,
            avatarPlaceholderTint = BrandIndigo,
            micActiveBackground = DeclineRed,
            signalStrong = AcceptGreen,
            signalWeak = Color(0xFFE07055),
        ),
        fonts = CallFonts(
            name = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            ),
            status = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
            ),
            button = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
            ),
            title = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            ),
        ),
        icons = CallIcons(
            accept = Icons.Filled.Phone,
            hangUp = Icons.Filled.CallEnd,
            decline = Icons.Filled.CallEnd,
            mute = Icons.Filled.Mic,
            unmute = Icons.Filled.MicOff,
            speaker = Icons.AutoMirrored.Filled.VolumeUp,
            earpiece = Icons.Filled.Call,
        ),
        gradientColors = listOf(
            Color(0xFFE8EAF6),
            Color(0xFFF3F5FF),
            Color(0xFFE3F2FD),
        ),
    )

    val strings = CallStrings(
        freeCallTitle = "Demo free call",
        connecting = "Connecting…",
        ringing = "Ringing…", 
        calling = "Calling…",
        callingYou = "Incoming call",
        inCall = "Connected",
        callEnded = "Call ended",
        userBusy = "User is busy",
        mute = "Mute",
        unmute = "Unmute",
        speaker = "Speaker",
        message = "Message",
        audioSourceTitle = "Audio source",
        strongSignal = "Strong signal",
        weakSignal = "Weak signal...",
    )
}
