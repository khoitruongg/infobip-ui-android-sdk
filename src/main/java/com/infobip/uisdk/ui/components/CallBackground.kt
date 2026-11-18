package com.infobip.uisdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.infobip.uisdk.ui.theme.currentCallAppearance

/**
 * Full-screen call background that applies [com.infobip.uisdk.ui.theme.CallAppearance]
 * gradient (or solid [com.infobip.uisdk.ui.theme.CallColors.surface] when gradient is null).
 *
 * Use this when building custom call screens from SDK building blocks.
 */
@Composable
fun CallBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val appearance = currentCallAppearance
    val gradient = appearance.gradientColors
    val backgroundModifier = if (gradient.isNullOrEmpty()) {
        modifier
            .fillMaxSize()
            .background(appearance.colors.surface)
    } else {
        modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(colors = gradient))
    }

    Box(modifier = backgroundModifier) {
        content()
    }
}
