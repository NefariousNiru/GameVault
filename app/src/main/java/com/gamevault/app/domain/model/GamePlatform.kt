package com.gamevault.app.domain.model

import kotlinx.serialization.Serializable

/** A library record belongs to exactly one platform at a time. */
@Serializable
enum class GamePlatform(val label: String) {
    PC("PC"), PS5("PS5")
}
