package com.gamevault.app

import android.app.Application
import com.gamevault.app.di.AppContainer

/** Application-wide, lazily initialized dependency container. */
class GameVaultApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
