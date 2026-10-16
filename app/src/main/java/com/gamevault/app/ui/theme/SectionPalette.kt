package com.gamevault.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.gamevault.app.domain.model.GamePlatform

data class SectionPalette(
    val accent: Color,
    val glow: Color,
    val background: Color,
    val secondBackground: Color,
    val dockTop: Color,
    val dockBottom: Color,
    val label: String
)

/** Route-aware visual identity. The themes stay glossy and dark, but each screen keeps its own color story. */
object VaultPalettes {
    val pc = SectionPalette(
        accent = Color(0xFF86E9FF),
        glow = Color(0xFF7A6BFF),
        background = Color(0xFF081320),
        secondBackground = Color(0xFF151133),
        dockTop = Color(0xFF121B2B),
        dockBottom = Color(0xFF0A101A),
        label = "PC LIBRARY"
    )
    val ps5 = SectionPalette(
        accent = Color(0xFF9FD4FF),
        glow = Color(0xFF3F7EFF),
        background = Color(0xFF071222),
        secondBackground = Color(0xFF15305D),
        dockTop = Color(0xFF102447),
        dockBottom = Color(0xFF0A1427),
        label = "PS5 LIBRARY"
    )
    val queue = SectionPalette(
        accent = Color(0xFFFFB184),
        glow = Color(0xFFFF7568),
        background = Color(0xFF211019),
        secondBackground = Color(0xFF3B1E22),
        dockTop = Color(0xFF27141B),
        dockBottom = Color(0xFF140A10),
        label = "PLAY QUEUE"
    )
    val settings = SectionPalette(
        accent = Color(0xFFA2DFD8),
        glow = Color(0xFF4F98A3),
        background = Color(0xFF0B1B21),
        secondBackground = Color(0xFF17303A),
        dockTop = Color(0xFF15272E),
        dockBottom = Color(0xFF0B151A),
        label = "SETTINGS"
    )

    fun forPlatform(platform: GamePlatform): SectionPalette =
        if (platform == GamePlatform.PC) pc else ps5
}
