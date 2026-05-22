package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkTerminalColorScheme = darkColorScheme(
    primary = NeonBluePrimary,
    secondary = NeonBlueSecondary,
    tertiary = TerminalAccentGlow,
    background = TerminalBackground,
    surface = TerminalCardBackground,
    onPrimary = TerminalBackground,
    onSecondary = TerminalTextPrimary,
    onBackground = TerminalTextPrimary,
    onSurface = TerminalTextPrimary,
    outline = BorderColor,
    error = NeonRedLoss
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Trading diary is strictly a professional dark terminal themed UI
    MaterialTheme(
        colorScheme = DarkTerminalColorScheme,
        typography = Typography,
        content = content
    )
}
