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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.gamevault.app.ui.theme.ArtworkMood
import com.gamevault.app.ui.theme.ArtworkPalette
import com.gamevault.app.ui.theme.SectionPalette
import kotlinx.coroutines.launch

/** Sample colors from the portrait album art to create a gradient-rich detail backdrop. */
@Composable
fun GameArtworkBackdrop(
    artworkUrl: String,
    fallback: SectionPalette,
    modifier: Modifier = Modifier,
    content: @Composable (ArtworkMood) -> Unit
) {
    var mood by remember(artworkUrl, fallback) { mutableStateOf(ArtworkMood.fallback(fallback)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(mood.top, mood.middle, mood.bottom))))
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(mood.accent.copy(alpha = 0.22f), androidx.compose.ui.graphics.Color.Transparent),
                    radius = 1100f
                )
            )
        )
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = remember(artworkUrl, context) {
                    ImageRequest.Builder(context).data(artworkUrl).allowHardware(false).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0f,
                onSuccess = { success ->
                    scope.launch {
                        mood = runCatching {
                            ArtworkPalette.mood(success.result.image.toBitmap(), fallback)
                        }.getOrElse { ArtworkMood.fallback(fallback) }
                    }
                }
            )
        }
        content(mood)
    }
}
