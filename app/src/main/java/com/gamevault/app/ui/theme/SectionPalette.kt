package com.gamevault.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.gamevault.app.domain.model.GamePlatform

/** Each top-level section has an independent, contrast-aware color identity. */
data class SectionPalette(
    val accent: Color,
    val glow: Color,
    val background: Color,
    val secondBackground: Color,
    val label: String
)

object VaultPalettes {
    // PC: AMD red meets NVIDIA green, grounded in near-black graphite.
    val pc = SectionPalette(
        accent = Color(0xFFFF6667),
        glow = Color(0xFF8AE34D),
        background = Color(0xFF190D12),
        secondBackground = Color(0xFF0C211B),
        label = "PC LIBRARY"
    )

    // PlayStation: Sony blue + clean white typography against midnight black.
    val ps5 = SectionPalette(
        accent = Color(0xFF8CCBFF),
        glow = Color(0xFF286BFF),
        background = Color(0xFF050B18),
        secondBackground = Color(0xFF123A75),
        label = "PS5 LIBRARY"
    )

    // A warm amber/sunset identity distinguishes the universal queue.
    val queue = SectionPalette(
        accent = Color(0xFFFFC28A),
        glow = Color(0xFFE26A56),
        background = Color(0xFF190F16),
        secondBackground = Color(0xFF352018),
        label = "PLAY QUEUE"
    )

    // Distinct muted teal/silver for account settings.
    val settings = SectionPalette(
        accent = Color(0xFF9CE5DE),
        glow = Color(0xFF397F91),
        background = Color(0xFF08151C),
        secondBackground = Color(0xFF1B303A),
        label = "SETTINGS"
    )

    fun forPlatform(platform: GamePlatform): SectionPalette = when (platform) {
        GamePlatform.PC -> pc
        GamePlatform.PS5 -> ps5
    }
}
