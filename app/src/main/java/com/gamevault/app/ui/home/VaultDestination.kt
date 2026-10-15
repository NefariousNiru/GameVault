package com.gamevault.app.ui.home

/** Navigation routes are intentionally separate from UI labels. */
enum class VaultDestination(val route: String, val label: String, val iconRes: Int) {
    LIBRARY("library", "Library", com.gamevault.app.R.drawable.gv_ic_library),
    QUEUE("queue", "Queue", com.gamevault.app.R.drawable.gv_ic_queue),
    SETTINGS("settings", "Settings", com.gamevault.app.R.drawable.gv_ic_settings)
}
