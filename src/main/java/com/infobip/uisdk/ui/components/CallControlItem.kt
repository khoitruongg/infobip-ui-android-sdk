package com.infobip.uisdk.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import com.infobip.uisdk.ui.theme.currentCallAppearance

@Composable
internal fun CallControlItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeIcon: ImageVector = icon,
    activeBackgroundColor: Color? = null,
    activeIconTint: Color? = null,
    inactiveBackgroundColor: Color? = null,
    inactiveIconTint: Color? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
    isActive: Boolean = false,
    enabled: Boolean = true,
) {
    val appearance = currentCallAppearance
    val colors = appearance.colors
    val inactiveBackground = inactiveBackgroundColor ?: colors.controlInactiveBackground
    val resolvedActiveBackground = activeBackgroundColor ?: colors.controlActiveBackground
    val resolvedActiveIconTint = activeIconTint ?: colors.iconOnControl
    val resolvedInactiveIconTint = inactiveIconTint ?: colors.iconInactive
    val displayedIcon = if (isActive) activeIcon else icon
    val backgroundColor = when {
        !enabled -> inactiveBackground.copy(alpha = 0.5f)
        isActive -> resolvedActiveBackground
        else -> inactiveBackground
    }
    val iconTint = when {
        !enabled -> colors.controlDisabled
        isActive -> resolvedActiveIconTint
        else -> resolvedInactiveIconTint
    }
    val resolvedLabelColor = when {
        !enabled -> colors.controlDisabled
        else -> labelColor ?: colors.textPrimary
    }

    Column(
        modifier = modifier.width(appearance.metrics.controlItemWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallSecondaryActionButton(
            icon = displayedIcon,
            backgroundColor = backgroundColor,
            iconTint = iconTint,
            onClick = onClick,
            size = appearance.metrics.controlIconDiameter,
            contentDescription = label,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(appearance.metrics.controlSpacing))
        Text(
            text = label,
            style = labelStyle ?: appearance.fonts.button,
            color = resolvedLabelColor,
        )
    }
}
