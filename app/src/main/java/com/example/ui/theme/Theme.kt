package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexaDarkColorScheme = darkColorScheme(
    primary = NexaNeonGreen,
    onPrimary = Color(0xFF002814),
    primaryContainer = NexaGreenDark,
    onPrimaryContainer = Color(0xFFE5FFF0),
    secondary = NexaTeal,
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF102D1C),
    onSecondaryContainer = Color(0xFF5EFFA8),
    tertiary = NexaGreenBright,
    background = NexaBlack,
    onBackground = NexaTextWhite,
    surface = NexaSurface,
    onSurface = NexaTextWhite,
    surfaceVariant = NexaSurfaceVariant,
    onSurfaceVariant = NexaTextMuted,
    outline = NexaGreenBorder,
    outlineVariant = NexaGreenBorderBright,
    error = NexaRed,
    errorContainer = NexaRedBg,
    onError = Color.White,
    onErrorContainer = Color(0xFFFF9E9E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Nexa AI uses its signature cyberpunk dark emerald visual identity
    MaterialTheme(
        colorScheme = NexaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
