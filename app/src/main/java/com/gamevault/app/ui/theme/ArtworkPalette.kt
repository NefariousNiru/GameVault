package com.gamevault.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Colors extracted on the device, never uploaded or persisted in Firestore. */
data class ArtworkMood(val accent: Color, val top: Color, val middle: Color, val bottom: Color) {
    companion object {
        fun fallback(section: SectionPalette) = ArtworkMood(
            accent = section.accent,
            top = section.background,
            middle = section.secondBackground,
            bottom = Color(0xFF080C13)
        )
    }
}

object ArtworkPalette {
    /**
     * Sample a small bitmap on Dispatchers.Default; avoid blocking the UI.
     * Pick separated prominent hues when available, otherwise create a subtle
     * analogous companion. Darkened swatches keep white content readable.
     */
    suspend fun mood(bitmap: Bitmap, fallback: SectionPalette): ArtworkMood = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).resizeBitmapArea(160 * 160).maximumColorCount(18).generate()
        val primary = (palette.vibrantSwatch ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch ?: palette.dominantSwatch)?.rgb
            ?: return@withContext ArtworkMood.fallback(fallback)
        val secondary = palette.swatches.sortedByDescending { it.population }
            .firstOrNull { swatch ->
                val a = FloatArray(3)
                val b = FloatArray(3)
                ColorUtils.colorToHSL(primary, a)
                ColorUtils.colorToHSL(swatch.rgb, b)
                val hueDiff = abs(a[0] - b[0]).let { minOf(it, 360f - it) }
                hueDiff >= 30f && b[1] >= 0.22f
            }?.rgb ?: run {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(primary, hsl)
                hsl[0] = (hsl[0] + 36f) % 360f
                hsl[1] = maxOf(0.35f, hsl[1])
                ColorUtils.HSLToColor(hsl)
            }
        val tint = ColorUtils.blendARGB(primary, android.graphics.Color.WHITE, 0.50f)
        ArtworkMood(
            accent = Color(tint),
            top = Color(ColorUtils.blendARGB(primary, android.graphics.Color.rgb(8, 11, 17), .77f)),
            middle = Color(ColorUtils.blendARGB(secondary, android.graphics.Color.rgb(8, 11, 17), .83f)),
            bottom = Color(0xFF080C13)
        )
    }
}
