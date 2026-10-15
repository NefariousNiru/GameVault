package com.gamevault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.gamevault.app.ui.theme.SectionPalette

/** A single shared backdrop ensures every route blends with the nav chrome. */
@Composable
fun VaultBackdrop(palette: SectionPalette, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(palette.background, palette.secondBackground, palette.background)
            )
        )
    ) { content() }
}
