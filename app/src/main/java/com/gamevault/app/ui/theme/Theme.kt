package com.gamevault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultDarkScheme = darkColorScheme(
    primary = VaultPalettes.pc.accent,
    onPrimary = VaultBackground,
    background = VaultBackground,
    onBackground = VaultText,
    surface = VaultSurface,
    onSurface = VaultText,
    surfaceVariant = VaultSurfaceElevated,
    onSurfaceVariant = VaultMuted,
    outline = VaultBorder
)

@Composable
fun GameVaultTheme(content: @Composable () -> Unit) {
    // Dark-first visual system. Optional light theme will be exposed in Settings later.
    MaterialTheme(colorScheme = VaultDarkScheme, typography = Typography, content = content)
}
