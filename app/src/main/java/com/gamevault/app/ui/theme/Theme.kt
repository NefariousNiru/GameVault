package com.gamevault.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Stage 3.1 will introduce PC, PS5, Queue, and artwork-derived themes.
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9DABFF),
    background = Color(0xFF0B0C12),
    surface = Color(0xFF171922),
    onBackground = Color(0xFFF1F1F6)
)
private val LightScheme = lightColorScheme()

@Composable
fun GameVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Intentionally NOT based on device wallpaper. Artwork-driven palettes follow in phase 3.1.
    @Suppress("UNUSED_VARIABLE")
    val unusedDynamicColor = dynamicColor
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content
    )
}
