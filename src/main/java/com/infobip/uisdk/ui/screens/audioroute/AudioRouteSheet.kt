package com.infobip.uisdk.ui.screens.audioroute

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.infobip.uisdk.model.AudioRoute
import com.infobip.uisdk.model.AudioRouteType
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings

/**
 * Bottom sheet listing available [AudioRoute]s for the active call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRouteSheet(
    routes: List<AudioRoute>,
    onSelect: (AudioRoute) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = appearance.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appearance.metrics.horizontalPadding)
                .padding(bottom = appearance.metrics.bottomSpacing),
        ) {
            Text(
                text = strings.audioSourceTitle,
                style = appearance.fonts.title,
                color = appearance.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            routes.forEach { route ->
                AudioRouteRow(
                    route = route,
                    icon = iconFor(route.type),
                    onClick = { onSelect(route) },
                )
            }
        }
    }
}

@Composable
private fun AudioRouteRow(
    route: AudioRoute,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val appearance = currentCallAppearance
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = route.name,
            tint = if (route.isActive) {
                appearance.colors.accent
            } else {
                appearance.colors.iconInactive
            },
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = route.name,
            style = appearance.fonts.status,
            color = if (route.isActive) {
                appearance.colors.accent
            } else {
                appearance.colors.textPrimary
            },
            modifier = Modifier.weight(1f),
        )
        if (route.isActive) {
            Text(
                text = "✓",
                color = appearance.colors.accent,
                style = appearance.fonts.title,
            )
        }
    }
}

@Composable
private fun iconFor(type: AudioRouteType): ImageVector {
    val icons = currentCallAppearance.icons
    return when (type) {
        AudioRouteType.Earpiece -> icons.earpiece
        AudioRouteType.Speaker -> icons.speaker
        AudioRouteType.Bluetooth -> icons.bluetooth
        AudioRouteType.Headphones -> icons.headphones
        AudioRouteType.Other -> icons.speaker
    }
}
