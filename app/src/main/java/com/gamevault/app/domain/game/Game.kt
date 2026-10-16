package com.gamevault.app.domain.game

import com.gamevault.app.domain.model.GamePlatform
import java.text.Normalizer
import java.util.Locale

/** All lifecycle dimensions are independent; skipping stages is supported. */
enum class ReleaseStatus(val label: String) { TBA("TBA"), ANNOUNCED("Announced"), UPCOMING("Upcoming"), RELEASED("Released") }
enum class InterestStatus(val label: String) { WISHLIST("Wishlist"), PLANNED("Planned"), DEFERRED("Deferred"), SKIPPED("Skipped") }
enum class Ps5Acquisition(val label: String) { NOT_OWNED("Not owned"), PREORDERED("Preordered"), PURCHASED("Purchased") }
enum class Ps5Library(val label: String) { NOT_ADDED("Not added"), IN_LIBRARY("In library") }
enum class PcAcquisition(val label: String) { NOT_DOWNLOADED("Not downloaded"), DOWNLOADED("Downloaded"), INSTALLED("Installed") }
enum class GameplayStatus(val label: String) { NOT_STARTED("Not started"), PLAYING("Playing"), ON_HOLD("On hold"), COMPLETED("Completed"), DROPPED("Dropped") }

/** Independent, immutable input. Platform-specific values are retained when switching platforms. */
data class GameDraft(
    val title: String = "",
    val platform: GamePlatform = GamePlatform.PC,
    val release: ReleaseStatus = ReleaseStatus.TBA,
    val interest: InterestStatus = InterestStatus.WISHLIST,
    val ps5Acquisition: Ps5Acquisition = Ps5Acquisition.NOT_OWNED,
    val ps5Library: Ps5Library = Ps5Library.NOT_ADDED,
    val pcAcquisition: PcAcquisition = PcAcquisition.NOT_DOWNLOADED,
    val gameplay: GameplayStatus = GameplayStatus.NOT_STARTED,
    val releaseDate: String = "",
    val notes: String = "",
    val rating: Int? = null,
    val completionDate: String = "",
    val coverUrl: String = "",
    val heroUrl: String = "",
    val queueRank: Long? = null,
    // Catalog metadata is separate from user-owned lifecycle fields.
    val rawgId: Int? = null,
    val rawgSlug: String = "",
    val developer: String = "",
    val publisher: String = "",
    val description: String = "",
    val genres: String = "",
    val tags: String = "",
    val supportedPlatforms: String = "",
    val rawgRating: Double? = null,
    val steamGridId: Int? = null
)

data class Game(
    val id: String,
    val titleKey: String,
    val draft: GameDraft,
    val updatedAtMillis: Long
)

/** Title normalization keeps duplicate detection consistent across app sessions. */
fun normalizeTitle(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
    .trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

fun GameDraft.validationError(): String? = when {
    title.trim().isBlank() -> "Enter a game title"
    title.length > 180 -> "Game title must be 180 characters or less"
    notes.length > 10_000 -> "Notes must be 10,000 characters or less"
    releaseDate.length > 32 || completionDate.length > 32 -> "Date text is too long"
    rating != null && rating !in 1..10 -> "Rating must be 1–10"
    rawgId != null && rawgId <= 0 -> "Invalid RAWG identifier"
    steamGridId != null && steamGridId <= 0 -> "Invalid SteamGridDB identifier"
    listOf(rawgSlug, developer, publisher, genres, tags, supportedPlatforms).any { it.length > 2000 } -> "Catalog field too long"
    description.length > 12000 -> "Description must be 12,000 characters or less"
    (rawgRating != null && (rawgRating < 0.0 || rawgRating > 5.0)) -> "RAWG rating out of range"
    !validArtworkUrl(coverUrl) || !validArtworkUrl(heroUrl) -> "Artwork links must use HTTPS"
    else -> null
}

private fun validArtworkUrl(url: String): Boolean = url.isBlank() ||
    (url.length <= 2_000 && url.startsWith("https://", ignoreCase = true))
