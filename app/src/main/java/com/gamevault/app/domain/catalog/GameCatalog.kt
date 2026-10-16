package com.gamevault.app.domain.catalog

/** Catalog metadata is advisory and never includes ownership, gameplay or personal ratings. */
data class CatalogMatch(
    val id: Int,
    val title: String,
    val releaseDate: String = "",
    val thumbnailUrl: String = ""
)

data class CatalogMetadata(
    val id: Int,
    val slug: String,
    val title: String,
    val releaseDate: String,
    val developer: String,
    val publisher: String,
    val description: String,
    val genres: String,
    val tags: String,
    val platforms: String,
    val rawgRating: Double?,
    val fallbackImage: String,
    val fallbackHero: String
)

data class ArtworkOption(val url: String, val thumbnail: String = url)

data class ArtworkChoices(
    val steamGridId: Int? = null,
    val matchedName: String = "",
    val exactMatch: Boolean = false,
    val covers: List<ArtworkOption> = emptyList(),
    val heroes: List<ArtworkOption> = emptyList()
)

interface GameCatalogRepository {
    suspend fun search(query: String, key: String): List<CatalogMatch>
    suspend fun details(id: Int, key: String): CatalogMetadata
    suspend fun artwork(title: String, key: String): ArtworkChoices
}
