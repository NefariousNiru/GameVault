package com.gamevault.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a readable accent from a cover/hero bitmap off the main thread.
 * The actual image-loading flow is wired when artwork records exist (Phase 4).
 */
object ArtworkPalette {
    suspend fun accent(bitmap: Bitmap, fallback: Color): Color = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.mutedSwatch
        val candidate = swatch?.rgb ?: return@withContext fallback
        // Ensure accents are readable on dark surfaces, even for very dark artwork.
        val readable = if (ColorUtils.calculateContrast(candidate, android.graphics.Color.rgb(8, 11, 19)) < 4.5) {
            lerp(Color(candidate), Color.White, 0.60f)
        } else {
            Color(candidate)
        }
        readable
    }
}
