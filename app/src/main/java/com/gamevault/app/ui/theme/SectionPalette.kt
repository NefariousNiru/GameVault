package com.gamevault.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.gamevault.app.domain.model.GamePlatform

/** Section-level colors used until an individual game's artwork takes precedence. */
data class SectionPalette(
    val accent: Color,
    val glow: Color,
    val background: Color,
    val secondBackground: Color,
    val label: String
)

object VaultPalettes {
    val pc = SectionPalette(
        Color(0xFFB49BFF), Color(0xFF7453ED),
        Color(0xFF090D1B), Color(0xFF13102A), "PC LIBRARY"
    )
    val ps5 = SectionPalette(
        Color(0xFF72BAFF), Color(0xFF306BF1),
        Color(0xFF071223), Color(0xFF0E1B3A), "PLAYSTATION 5"
    )
    val queue = SectionPalette(
        Color(0xFFFFC588), Color(0xFFFF8955),
        Color(0xFF151016), Color(0xFF221724), "THE NEXT CHAPTER"
    )
    val settings = SectionPalette(
        Color(0xFFB8C9E9), Color(0xFF536A8F),
        Color(0xFF090D17), Color(0xFF121924), "YOUR SPACE"
    )

    fun forPlatform(platform: GamePlatform) = when (platform) {
        GamePlatform.PC -> pc
        GamePlatform.PS5 -> ps5
    }
}
