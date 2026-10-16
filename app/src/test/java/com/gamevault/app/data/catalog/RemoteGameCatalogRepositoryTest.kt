package com.gamevault.app.data.catalog

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * HTTP contract tests for every external API route called by GameVault.
 * These tests use loopback mock servers. They never call RAWG or SteamGridDB,
 * require no accounts or keys, and do not consume API quotas.
 */
class RemoteGameCatalogRepositoryTest {
    private lateinit var rawgServer: MockWebServer
    private lateinit var steamServer: MockWebServer
    private lateinit var subject: RemoteGameCatalogRepository

    @Before fun setUp() {
        rawgServer = MockWebServer().apply { start() }
        steamServer = MockWebServer().apply { start() }
        subject = RemoteGameCatalogRepository(
            rawgServer.url("/api/").toString(),
            steamServer.url("/api/v2/").toString()
        )
    }

    @After fun tearDown() {
        rawgServer.shutdown()
        steamServer.shutdown()
    }

    private fun MockWebServer.enqueueJson(body: String, code: Int = 200) {
        enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    private fun MockWebServer.takeRequestOrFail() =
        takeRequest(3, TimeUnit.SECONDS) ?: error("No API request received")

    @Test fun `RAWG search hits games route with key query and page size`() = runBlocking {
        rawgServer.enqueueJson("""
            {"results":[
              {"id": 42, "name":"Elden Ring", "released":"2022-02-25", "background_image":"https://img.example/one.jpg"},
              {"id": 42, "name":"Elden Ring"},
              {"id": 43, "name":"Elden Ring Nightreign", "released":"2025-05-30"},
              {"name":"No ID"}, {"id": 44, "name":""}
            ]}
        """.trimIndent())

        val results = subject.search("  Elden Ring  ", "  local-test-key ")

        assertEquals(listOf(42, 43), results.map { it.id })
        assertEquals("Elden Ring", results[0].title)
        assertEquals("https://img.example/one.jpg", results[0].thumbnailUrl)
        val request = rawgServer.takeRequestOrFail()
        assertEquals("GET", request.method)
        assertEquals("/api/games", request.requestUrl?.encodedPath)
        assertEquals("local-test-key", request.requestUrl?.queryParameter("key"))
        assertEquals("Elden Ring", request.requestUrl?.queryParameter("search"))
        assertEquals("10", request.requestUrl?.queryParameter("page_size"))
    }

    @Test fun `RAWG empty or missing results return empty list`() = runBlocking {
        rawgServer.enqueueJson("{}")
        assertTrue(subject.search("Mystery", "key").isEmpty())
        rawgServer.enqueueJson("""{"results": []}""")
        assertTrue(subject.search("Mystery", "key").isEmpty())
    }

    @Test fun `RAWG details extracts all available fields and supports plain description`() = runBlocking {
        rawgServer.enqueueJson("""
            {
              "id":42,"slug":"elden-ring","name":"Elden Ring","released":"2022-02-25",
              "developers":[{"name":"FromSoftware"},{"name":"FromSoftware"}],
              "publishers":[{"name":"Bandai Namco"}],
              "genres":[{"name":"Action"},{"name":"RPG"}],
              "tags":[{"name":"Open World"},{"name":"Soulslike"}],
              "platforms":[{"platform":{"name":"PC"}},{"platform":{"name":"PlayStation 5"}}],
              "description_raw":"Explore the Lands Between.","rating":4.52,
              "background_image":"https://img.example/album.jpg",
              "background_image_additional":"https://img.example/landscape.jpg"
            }
        """.trimIndent())
        val details = subject.details(42, " key ")
        assertEquals(42, details.id)
        assertEquals("elden-ring", details.slug)
        assertEquals("Elden Ring", details.title)
        assertEquals("2022-02-25", details.releaseDate)
        assertEquals("FromSoftware", details.developer)
        assertEquals("Bandai Namco", details.publisher)
        assertEquals("Action, RPG", details.genres)
        assertEquals("Open World, Soulslike", details.tags)
        assertEquals("PC, PlayStation 5", details.platforms)
        assertEquals("Explore the Lands Between.", details.description)
        assertEquals(4.52, details.rawgRating!!, 0.0001)
        assertEquals("https://img.example/album.jpg", details.fallbackImage)
        assertEquals("https://img.example/landscape.jpg", details.fallbackHero)
        val request = rawgServer.takeRequestOrFail()
        assertEquals("/api/games/42", request.requestUrl?.encodedPath)
        assertEquals("key", request.requestUrl?.queryParameter("key"))
    }

    @Test fun `RAWG missing optional metadata yields safe defaults`() = runBlocking {
        rawgServer.enqueueJson("""{"name":"Unknown", "description":"<p>Short <b>story</b></p>"}""")
        val details = subject.details(100, "key")
        assertEquals("Short  story", details.description) // strips HTML tags; not a rich text renderer
        assertEquals("", details.publisher)
        assertEquals("", details.tags)
        assertEquals("", details.platforms)
        assertNull(details.rawgRating)
        assertEquals("", details.fallbackImage)
        assertEquals("", details.fallbackHero)
    }

    @Test fun `RAWG background additional absent falls back to background image`() = runBlocking {
        rawgServer.enqueueJson("""{"name":"Unknown", "background_image":"https://img.example/fallback.jpg"}""")
        val details = subject.details(100, "key")
        assertEquals(details.fallbackImage, details.fallbackHero)
    }

    @Test fun `RAWG missing key fails before network request`() = runBlocking {
        try {
            subject.search("Game", " ")
            fail("Missing RAWG key must fail")
        } catch (_: IllegalArgumentException) { }
        try {
            subject.details(1, "")
            fail("Missing RAWG key must fail")
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, rawgServer.requestCount)
    }

    @Test fun `RAWG auth error and rate limit are surfaced without being swallowed`() = runBlocking {
        for (status in listOf(401, 403, 429, 500)) {
            rawgServer.enqueueJson("""{"error":"test response"}""", status)
            try {
                subject.search("Game", "bad-key")
                fail("Expected HTTP $status")
            } catch (error: HttpException) {
                assertEquals(status, error.code())
            }
        }
    }

    @Test fun `RAWG malformed JSON fails rather than persisting broken metadata`() = runBlocking {
        rawgServer.enqueueJson("not-json")
        try {
            subject.details(10, "key")
            fail("Malformed JSON should fail")
        } catch (_: Exception) { }
    }

    @Test fun `SteamGridDB exact match fetches portrait grids and landscape heroes`() = runBlocking {
        steamServer.enqueueJson("""{"success":true,"data":[{"id":987,"name":"Elden Ring"}]}""")
        steamServer.enqueueJson("""{"success":true,"data":[
          {"url":"https://cdn.example/cover.webp","thumb":"https://cdn.example/cover-small.webp"},
          {"url":"https://cdn.example/cover.webp"}, {"url":"http://not-secure.example/file"}
        ]}""")
        steamServer.enqueueJson("""{"success":true,"data":[{"url":"https://cdn.example/hero.png"}]}""")

        val art = subject.artwork(" Elden Ring ", " test-token ")
        assertTrue(art.exactMatch)
        assertEquals("Elden Ring", art.matchedName)
        assertEquals(987, art.steamGridId)
        assertEquals(1, art.covers.size)
        assertEquals("https://cdn.example/cover-small.webp", art.covers.first().thumbnail)
        assertEquals("https://cdn.example/hero.png", art.heroes.first().url)
        val search = steamServer.takeRequestOrFail()
        assertEquals("/api/v2/search/autocomplete/Elden%20Ring", search.requestUrl?.encodedPath)
        assertEquals("Bearer test-token", search.getHeader("Authorization"))
        val grids = steamServer.takeRequestOrFail()
        assertEquals("/api/v2/grids/game/987", grids.requestUrl?.encodedPath)
        assertEquals("600x900,342x482,660x930", grids.requestUrl?.queryParameter("dimensions"))
        assertEquals("static", grids.requestUrl?.queryParameter("types"))
        assertEquals("12", grids.requestUrl?.queryParameter("limit"))
        assertEquals("Bearer test-token", grids.getHeader("Authorization"))
        val heroes = steamServer.takeRequestOrFail()
        assertEquals("/api/v2/heroes/game/987", heroes.requestUrl?.encodedPath)
        assertEquals("static", heroes.requestUrl?.queryParameter("types"))
        assertEquals("12", heroes.requestUrl?.queryParameter("limit"))
    }

    @Test fun `SteamGridDB mismatched edition never auto-associates artwork`() = runBlocking {
        steamServer.enqueueJson("""{"success":true,"data":[{"id":10,"name":"Elden Ring Nightreign"}]}""")
        val result = subject.artwork("Elden Ring", "token")
        assertFalse(result.exactMatch)
        assertEquals("Elden Ring Nightreign", result.matchedName)
        assertTrue(result.covers.isEmpty())
        assertTrue(result.heroes.isEmpty())
        assertEquals(1, steamServer.requestCount)
    }

    @Test fun `SteamGridDB case and whitespace differences are acceptable`() = runBlocking {
        steamServer.enqueueJson("""{"data":[{"id":77,"name":"  ELDEN   RING  "}]}""")
        steamServer.enqueueJson("""{"data":[]}""")
        steamServer.enqueueJson("""{"data":[]}""")
        assertTrue(subject.artwork("Elden Ring", "token").exactMatch)
    }

    @Test fun `SteamGridDB 404 search returns no artwork`() = runBlocking {
        steamServer.enqueueJson("""{"error":"missing"}""", 404)
        val result = subject.artwork("Game", "token")
        assertTrue(result.covers.isEmpty())
        assertNull(result.steamGridId)
    }

    @Test fun `SteamGridDB 404 cover still loads hero`() = runBlocking {
        steamServer.enqueueJson("""{"data":[{"id":55,"name":"Game"}]}""")
        steamServer.enqueueJson("""{"error":"missing"}""", 404)
        steamServer.enqueueJson("""{"data":[{"url":"https://cdn.example/hero.webp"}]}""")
        val art = subject.artwork("Game", "token")
        assertTrue(art.covers.isEmpty())
        assertEquals(1, art.heroes.size)
    }

    @Test fun `SteamGridDB 404 hero still loads cover`() = runBlocking {
        steamServer.enqueueJson("""{"data":[{"id":55,"name":"Game"}]}""")
        steamServer.enqueueJson("""{"data":[{"url":"https://cdn.example/album.webp"}]}""")
        steamServer.enqueueJson("""{"error":"missing"}""", 404)
        val art = subject.artwork("Game", "token")
        assertEquals(1, art.covers.size)
        assertTrue(art.heroes.isEmpty())
    }

    @Test fun `SteamGridDB 401 403 429 and 5xx propagate to UI`() = runBlocking {
        for (status in listOf(401, 403, 429, 500)) {
            steamServer.enqueueJson("""{"error":"failure"}""", status)
            try {
                subject.artwork("Game", "bad-token")
                fail("Expected HTTP $status")
            } catch (error: HttpException) {
                assertEquals(status, error.code())
            }
        }
    }

    @Test fun `SteamGridDB missing key returns empty without network`() = runBlocking {
        assertEquals(0, subject.artwork("Game", "  ").covers.size)
        assertEquals(0, steamServer.requestCount)
    }

    @Test fun `SteamGridDB search with empty data returns empty`() = runBlocking {
        steamServer.enqueueJson("""{"success":true,"data":[]}""")
        val result = subject.artwork("Unlisted Game", "token")
        assertFalse(result.exactMatch)
        assertTrue(result.covers.isEmpty())
        assertEquals(1, steamServer.requestCount)
    }

    @Test fun `transport failure is surfaced for both providers`() = runBlocking {
        val unusedPort = ServerSocket(0).use { it.localPort }
        val offline = RemoteGameCatalogRepository(
            "http://127.0.0.1:$unusedPort/api/",
            "http://127.0.0.1:$unusedPort/api/v2/"
        )
        try {
            offline.search("Game", "key")
            fail("Disconnected RAWG API must fail")
        } catch (_: IOException) { }
        try {
            offline.artwork("Game", "key")
            fail("Disconnected SteamGridDB API must fail")
        } catch (_: IOException) { }
    }

}
