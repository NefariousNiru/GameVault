package com.gamevault.app.data.catalog

import com.gamevault.app.domain.catalog.*
import com.gamevault.app.domain.game.normalizeTitle
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/** External APIs are strictly read-only; no calls ever write to Firebase. */
internal interface RawgApi {
    @GET("games")
    suspend fun search(@Query("key") key: String, @Query("search") query: String,
                       @Query("page_size") limit: Int = 10): JsonObject
    @GET("games/{id}")
    suspend fun details(@Path("id") id: Int, @Query("key") key: String): JsonObject
}

internal interface SteamGridApi {
    @GET("search/autocomplete/{term}")
    suspend fun search(@Path("term") term: String, @Header("Authorization") token: String): JsonObject
    @GET("grids/game/{id}")
    suspend fun covers(@Path("id") id: Int, @Header("Authorization") token: String,
                       @Query("dimensions") dimensions: String = "600x900,342x482,660x930",
                       @Query("types") types: String = "static", @Query("limit") limit: Int = 12): JsonObject
    @GET("heroes/game/{id}")
    suspend fun heroes(@Path("id") id: Int, @Header("Authorization") token: String,
                       @Query("types") types: String = "static", @Query("limit") limit: Int = 12): JsonObject
}

class RemoteGameCatalogRepository : GameCatalogRepository {
    private val rawg: RawgApi = Retrofit.Builder().baseUrl("https://api.rawg.io/api/")
        .addConverterFactory(GsonConverterFactory.create()).build().create(RawgApi::class.java)
    private val steam: SteamGridApi = Retrofit.Builder().baseUrl("https://www.steamgriddb.com/api/v2/")
        .addConverterFactory(GsonConverterFactory.create()).build().create(SteamGridApi::class.java)

    override suspend fun search(query: String, key: String): List<CatalogMatch> {
        require(key.isNotBlank()) { "Add your RAWG API key in Settings first" }
        val response = rawg.search(key.trim(), query.trim())
        return response.array("results").mapNotNull { item ->
            val id = item.int("id") ?: return@mapNotNull null
            CatalogMatch(id, item.str("name"), item.str("released"), item.str("background_image"))
        }.filter { it.title.isNotBlank() }.distinctBy { it.id }
    }

    override suspend fun details(id: Int, key: String): CatalogMetadata {
        require(key.isNotBlank()) { "Add your RAWG API key in Settings first" }
        val item = rawg.details(id, key.trim())
        return CatalogMetadata(
            id = id,
            slug = item.str("slug"),
            title = item.str("name"),
            releaseDate = item.str("released"),
            developer = item.names("developers"),
            publisher = item.names("publishers"),
            description = item.str("description_raw").ifBlank { item.str("description").replace(Regex("<[^>]*>"), " ").trim() },
            genres = item.names("genres"),
            tags = item.names("tags"),
            platforms = item.array("platforms").mapNotNull { entry -> entry.obj("platform")?.str("name")?.takeIf { it.isNotBlank() } }.distinct().joinToString(", "),
            rawgRating = item.get("rating")?.takeUnless { it.isJsonNull }?.asDouble,
            fallbackImage = item.str("background_image"),
            fallbackHero = item.str("background_image_additional").ifBlank { item.str("background_image") }
        )
    }

    override suspend fun artwork(title: String, key: String): ArtworkChoices {
        if (key.isBlank()) return ArtworkChoices()
        val matches = try { steam.search(title.trim(), "Bearer ${key.trim()}").array("data") }
        catch (error: HttpException) { if (error.code() == 404) return ArtworkChoices() else throw error }
        // Only automatically associate artwork when the normalized title agrees. Do not silently
        // attach a similarly named sequel/edition. Users can still set custom URLs manually.
        val selected = matches.firstOrNull { normalizeTitle(it.str("name")) == normalizeTitle(title) }
            ?: return ArtworkChoices(matchedName = matches.firstOrNull()?.str("name") ?: "")
        val id = selected.int("id") ?: return ArtworkChoices()
        val auth = "Bearer ${key.trim()}"
        val covers = try { steam.covers(id, auth).array("data").mapImages() }
            catch (error: HttpException) { if (error.code() == 404) emptyList() else throw error }
        val heroes = try { steam.heroes(id, auth).array("data").mapImages() }
            catch (error: HttpException) { if (error.code() == 404) emptyList() else throw error }
        return ArtworkChoices(id, selected.str("name"), true, covers, heroes)
    }
}

private fun JsonObject.str(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
private fun JsonObject.int(key: String): Int? = get(key)?.takeUnless { it.isJsonNull }?.asInt
private fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonObject.array(key: String): List<JsonObject> =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }.orEmpty()
private fun JsonObject.names(key: String): String = array(key).map { it.str("name") }.filter { it.isNotBlank() }.distinct().joinToString(", ")
private fun List<JsonObject>.mapImages(): List<ArtworkOption> = mapNotNull { it ->
    it.str("url").takeIf { url -> url.startsWith("https://") }?.let { url ->
        ArtworkOption(url, it.str("thumb").ifBlank { url })
    }
}.distinctBy { it.url }
