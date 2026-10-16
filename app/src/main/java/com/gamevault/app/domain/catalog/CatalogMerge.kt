package com.gamevault.app.domain.catalog

import com.gamevault.app.domain.game.GameDraft
import com.gamevault.app.domain.game.ReleaseStatus
import java.time.LocalDate

/** Only public catalog fields are refreshed. Personal lifecycle, notes and rating are immutable. */
fun GameDraft.applyCatalog(metadata: CatalogMetadata, today: LocalDate = LocalDate.now()): GameDraft {
    val linkedBefore = rawgId == metadata.id
    val releaseStatus = if (release != ReleaseStatus.TBA) release else {
        if (metadata.releaseDate.isBlank()) ReleaseStatus.TBA
        else if (metadata.releaseDate > today.toString()) ReleaseStatus.UPCOMING
        else ReleaseStatus.RELEASED
    }
    return copy(
        title = metadata.title, rawgId = metadata.id, rawgSlug = metadata.slug,
        releaseDate = metadata.releaseDate, release = releaseStatus,
        developer = metadata.developer, publisher = metadata.publisher,
        description = metadata.description, genres = metadata.genres, tags = metadata.tags,
        supportedPlatforms = metadata.platforms, rawgRating = metadata.rawgRating,
        steamGridId = if (linkedBefore) steamGridId else null,
        coverUrl = if (linkedBefore && coverUrl.isNotBlank()) coverUrl else metadata.fallbackImage,
        heroUrl = if (linkedBefore && heroUrl.isNotBlank()) heroUrl else metadata.fallbackHero
    )
}
