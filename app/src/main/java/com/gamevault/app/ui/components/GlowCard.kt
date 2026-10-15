package com.gamevault.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.gamevault.app.ui.theme.SectionPalette

/** Reusable translucent card surface for empty states and settings. */
@Composable
fun GlowCard(palette: SectionPalette, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier.fillMaxWidth().clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF222B3A).copy(alpha = 0.78f), palette.glow.copy(alpha = 0.10f), Color(0xFF121722))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)), shape)
    ) { content() }
}
