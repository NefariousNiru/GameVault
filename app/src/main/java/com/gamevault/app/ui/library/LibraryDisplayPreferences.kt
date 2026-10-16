package com.gamevault.app.ui.library

import com.gamevault.app.domain.model.GamePlatform

/** Device-only presentation choices. Game/queue records remain in Firestore. */
data class LibraryDisplayPreferences(
    val query: String = "",
    val grid: Boolean = true,
    val filter: String = "All",
    val sort: GameSort = GameSort.TITLE_ASC
)

data class LocalDisplayPreferences(
    val platform: GamePlatform = GamePlatform.PC,
    val route: String = "library",
    val pc: LibraryDisplayPreferences = LibraryDisplayPreferences(),
    val ps5: LibraryDisplayPreferences = LibraryDisplayPreferences()
) {
    fun forPlatform(value: GamePlatform): LibraryDisplayPreferences =
        if (value == GamePlatform.PC) pc else ps5

    fun withPlatformPreferences(value: GamePlatform, preferences: LibraryDisplayPreferences): LocalDisplayPreferences =
        if (value == GamePlatform.PC) copy(pc = preferences) else copy(ps5 = preferences)
}

/** Invalid/stale disk values must never break rendering after an upgrade. */
fun restoredLibraryFilter(raw: String?): String =
    raw?.takeIf { it in setOf("All", "Playing", "Completed", "Wishlist") } ?: "All"

fun restoredLibrarySort(raw: String?): GameSort =
    GameSort.entries.firstOrNull { it.name == raw } ?: GameSort.TITLE_ASC

fun restoredLibraryRoute(raw: String?): String =
    raw?.takeIf { it in setOf("library", "queue", "settings") } ?: "library"

fun restoredLibraryPlatform(raw: String?): GamePlatform =
    GamePlatform.entries.firstOrNull { it.name == raw } ?: GamePlatform.PC
