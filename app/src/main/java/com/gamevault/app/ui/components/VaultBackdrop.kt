package com.gamevault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.gamevault.app.ui.theme.SectionPalette

/** One route-aware backdrop; PC, PS5, Queue, and Settings are visually distinct. */
@Composable
fun VaultBackdrop(palette: SectionPalette, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(palette.background, palette.secondBackground, palette.background)
            )
        )
    ) { content() }
}
