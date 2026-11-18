package com.infobip.uisdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.infobip.uisdk.model.CallParticipant
import com.infobip.uisdk.ui.theme.currentCallAppearance

@Composable
fun CallAvatar(
    participant: CallParticipant,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    placeholderIcon: ImageVector? = null,
    placeholderBackground: Color? = null,
    placeholderTint: Color? = null,
    initialsStyle: TextStyle? = null,
    initialsColor: Color? = null,
) {
    CallAvatar(
        name = participant.displayName,
        avatarUrl = participant.avatarUrl,
        modifier = modifier,
        size = size,
        placeholderIcon = placeholderIcon,
        placeholderBackground = placeholderBackground,
        placeholderTint = placeholderTint,
        initialsStyle = initialsStyle,
        initialsColor = initialsColor,
    )
}

@Composable
fun CallAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    placeholderIcon: ImageVector? = null,
    placeholderBackground: Color? = null,
    placeholderTint: Color? = null,
    initialsStyle: TextStyle? = null,
    initialsColor: Color? = null,
) {
    val appearance = currentCallAppearance
    val resolvedSize = size ?: appearance.metrics.avatarSize
    val shape = RoundedCornerShape(appearance.metrics.avatarCornerRadius)

    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = modifier
                .size(resolvedSize)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        val initials = name
            .trim()
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")

        Box(
            modifier = modifier
                .size(resolvedSize)
                .clip(shape)
                .background(placeholderBackground ?: appearance.colors.avatarPlaceholderBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (initials.isBlank()) {
                Icon(
                    imageVector = placeholderIcon ?: appearance.icons.avatarPlaceholder,
                    contentDescription = name,
                    tint = placeholderTint ?: appearance.colors.avatarPlaceholderTint,
                    modifier = Modifier.size(resolvedSize * 0.4f),
                )
            } else {
                Text(
                    text = initials,
                    style = initialsStyle
                        ?: appearance.fonts.name.copy(fontWeight = FontWeight.Bold),
                    color = initialsColor ?: appearance.colors.textPrimary,
                )
            }
        }
    }
}
