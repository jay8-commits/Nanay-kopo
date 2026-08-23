package com.kmjs.virtualcamera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VibrantColorScheme = lightColorScheme(
    primary = VibrantPurple,
    onPrimary = Color.White,
    primaryContainer = VibrantPurpleLight,
    onPrimaryContainer = VibrantPurpleDark,
    secondary = VibrantPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = VibrantPurplePill,
    onSecondaryContainer = VibrantPurpleDark,
    background = VibrantBackground,
    onBackground = TextPrimary,
    surface = VibrantSurface,
    onSurface = TextPrimary,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VibrantCardBorder,
    error = VibrantLatencyAlert,
    onError = Color.White,
    errorContainer = VibrantErrorContainer,
    onErrorContainer = VibrantLatencyAlert
)

@Composable
fun KMJSVirtualCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibrantColorScheme,
        typography = Typography,
        content = content
    )
}

