package com.gamevault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.gamevault.app.ui.theme.SectionPalette

/** Rich dark shell with glossy layered gradients for each route. */
@Composable
fun VaultBackdrop(palette: SectionPalette, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(palette.background, palette.secondBackground, palette.background)
            )
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(180f, 80f),
                    radius = 1100f
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(palette.glow.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(1000f, 280f),
                    radius = 1200f
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent, Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(900f, 320f)
                )
            )
        )
        content()
    }
}
