package com.gamevault.app.domain.model

/** A library record belongs to exactly one platform at a time. */
enum class GamePlatform(val label: String) {
    PC("PC"), PS5("PS5")
}
