package com.gamevault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.gamevault.app.ui.theme.ArtworkMood
import com.gamevault.app.ui.theme.ArtworkPalette
import com.gamevault.app.ui.theme.SectionPalette
import kotlinx.coroutines.launch

/**
 * Full-height cinematic background for game details.
 * The image is dimmed beneath dark surfaces; swatches color the surrounding
 * gradient using the decoded Coil image already in cache (no second fetch).
 */
@Composable
fun GameArtworkBackdrop(
    heroUrl: String,
    fallback: SectionPalette,
    modifier: Modifier = Modifier,
    content: @Composable (ArtworkMood) -> Unit
) {
    var mood by remember(heroUrl, fallback) { mutableStateOf(ArtworkMood.fallback(fallback)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(mood.top, mood.middle, mood.bottom))))
        if (heroUrl.isNotBlank()) {
            AsyncImage(
                model = remember(heroUrl) {
                    ImageRequest.Builder(context).data(heroUrl).allowHardware(false)
                        .crossfade(true).build()
                },
                contentDescription = null, // Decorative behind readable foreground content.
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = .56f,
                onSuccess = { success ->
                    val image = success.result.image
                    scope.launch { mood = ArtworkPalette.mood(image.toBitmap(), fallback) }
                }
            )
        }
        // Keep all text readable even with high-contrast or bright user artwork.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color(0x68050910), Color(0xD00A0E15), Color(0xF7080C13)
        ))))
        content(mood)
    }
}
