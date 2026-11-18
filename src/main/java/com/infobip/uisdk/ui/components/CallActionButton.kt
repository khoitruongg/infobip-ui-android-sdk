package com.infobip.uisdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.infobip.uisdk.ui.theme.currentCallAppearance

@Composable
internal fun CallPrimaryActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    iconTint: Color? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val appearance = currentCallAppearance
    val resolvedSize = size ?: appearance.metrics.actionButtonDiameter
    val resolvedTint = iconTint ?: appearance.colors.iconOnControl
    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = if (enabled) 1f else 0.5f))
            .noRippleClickable { if (enabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(resolvedSize * 0.4f),
        )
    }
}

@Composable
internal fun CallSecondaryActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    iconTint: Color? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val appearance = currentCallAppearance
    val resolvedSize = size ?: appearance.metrics.controlIconDiameter
    val resolvedTint = iconTint ?: appearance.colors.primary
    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = if (enabled) 1f else 0.5f))
            .noRippleClickable { if (enabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(resolvedSize * 0.45f),
        )
    }
}
