package com.infobip.uisdk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalCallAppearance = staticCompositionLocalOf { CallAppearance.Default }
internal val LocalCallStrings = staticCompositionLocalOf { CallStrings.Default }

@Composable
internal fun InfobipCallTheme(
    appearance: CallAppearance = CallAppearance.Default,
    strings: CallStrings = CallStrings.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = appearance.colors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.iconOnControl,
            secondary = colors.accept,
            background = colors.surface,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.accept,
            background = colors.surface,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
        )
    }

    val typography = CallTypography.copy(
        headlineMedium = appearance.fonts.name,
        titleMedium = appearance.fonts.title,
        bodyLarge = appearance.fonts.status,
        labelMedium = appearance.fonts.caption,
    )

    CompositionLocalProvider(
        LocalCallAppearance provides appearance,
        LocalCallStrings provides strings,
        // Keep LocalCallUiTheme in sync for any lingering CallUiTheme readers.
        LocalCallUiTheme provides CallUiTheme.from(appearance),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

internal val LocalCallUiTheme = staticCompositionLocalOf { CallUiTheme.Default }

val currentCallAppearance: CallAppearance
    @Composable get() = LocalCallAppearance.current

val currentCallStrings: CallStrings
    @Composable get() = LocalCallStrings.current

internal val currentCallUiTheme: CallUiTheme
    @Composable get() = LocalCallUiTheme.current
