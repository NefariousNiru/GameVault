package com.gamevault.app.data.artwork

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.gamevault.app.domain.game.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Prefetch covers and landscapes sequentially to avoid spikes in network or memory use. */
class ArtworkPrefetcher(private val context: Context) {
    suspend fun prefetch(games: List<Game>): Int = withContext(Dispatchers.IO) {
        val loader = SingletonImageLoader.get(context)
        val urls = games.flatMap { listOf(it.draft.coverUrl, it.draft.heroUrl) }
            .filter { it.startsWith("https://", ignoreCase = true) }.distinct()
        var successful = 0
        for (url in urls) {
            try {
                val result = loader.execute(ImageRequest.Builder(context).data(url).build())
                if (result is coil3.request.SuccessResult) successful++
            } catch (_: Exception) {
                // A missing cover should never block the rest of the library.
            }
        }
        successful
    }
}
