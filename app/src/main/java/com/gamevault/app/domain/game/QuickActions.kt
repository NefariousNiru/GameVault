package com.gamevault.app.domain.game

/** Only independently user-owned lifecycle fields can be quick-edited. */
enum class QuickStatusField(val firestoreKey: String) {
    PLAY_STATUS("gameplay"),
    INTEREST("interest"),
    PC_DOWNLOAD("pcAcquisition"),
    PS5_ACCESS("ps5Acquisition")
}

/** Validate the intended enum and never trust a string coming from the UI. */
fun QuickStatusField.isAllowed(value: String): Boolean = when (this) {
    QuickStatusField.PLAY_STATUS -> GameplayStatus.entries.any { it.name == value }
    QuickStatusField.INTEREST -> InterestStatus.entries.any { it.name == value }
    QuickStatusField.PC_DOWNLOAD -> PcAcquisition.entries.any { it.name == value }
    QuickStatusField.PS5_ACCESS -> Ps5Acquisition.entries.any { it.name == value }
}

/** Pure reorder function: preserving existing membership; invalid IDs are rejected. */
fun moveQueuedId(ids: List<String>, from: Int, to: Int): List<String> {
    require(ids.distinct().size == ids.size) { "Duplicate queue IDs" }
    require(from in ids.indices && to in ids.indices) { "Invalid queue position" }
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}
