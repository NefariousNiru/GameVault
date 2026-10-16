package com.gamevault.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import com.gamevault.app.di.AppContainer

/** One global artwork loader with a bounded, persistent app-private disk cache. */
class GameVaultApplication : Application(), SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.filesDir.resolve("gamevault_artwork"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .build()
}
