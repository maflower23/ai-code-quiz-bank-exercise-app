package com.example.quizapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PureBlackColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = PureBlack,
    primaryContainer = AccentGreenDark,
    onPrimaryContainer = TextPrimary,
    secondary = WarningAmber,
    onSecondary = PureBlack,
    tertiary = Color(0xFF80CBC4),
    background = PureBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = AccentGreen,
    error = ErrorRed,
    onError = PureBlack,
    outline = Color(0xFF3A3A3A),
)

@Composable
fun QuizTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PureBlackColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
