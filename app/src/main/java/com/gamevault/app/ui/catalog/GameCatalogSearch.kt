package com.gamevault.app.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gamevault.app.data.catalog.CatalogKeys
import com.gamevault.app.domain.catalog.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Single search-first entry point. Debounce/cancellation ensures stale search responses
 * cannot override a newer query; selection is similarly keyed to the chosen RAWG ID.
 * Existing personal fields are never modified here.
 */
@Composable
fun GameCatalogSearch(
    keys: CatalogKeys,
    catalog: GameCatalogRepository,
    currentRawgId: Int?,
    onMetadata: (CatalogMetadata) -> Unit,
    onArtwork: (ArtworkChoices) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(emptyList<CatalogMatch>()) }
    var selected by remember { mutableStateOf<CatalogMatch?>(null) }
    var loadingSearch by remember { mutableStateOf(false) }
    var searchCompleted by remember { mutableStateOf(false) }
    var loadingDetails by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedError by remember { mutableStateOf<String?>(null) }
    var retrySerial by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, keys.rawg) {
        matches = emptyList()
        searchError = null
        loadingSearch = false
        searchCompleted = false
        if (query.trim().length < 2 || keys.rawg.isBlank() || selected != null) return@LaunchedEffect
        delay(400)
        loadingSearch = true
        try {
            matches = catalog.search(query, keys.rawg)
            searchCompleted = true
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            searchError = readableCatalogError(error)
        } finally {
            loadingSearch = false
        }
    }
    LaunchedEffect(selected?.id, keys.rawg, retrySerial) {
        val match = selected ?: return@LaunchedEffect
        selectedError = null
        loadingDetails = true
        try {
            onMetadata(catalog.details(match.id, keys.rawg))
            // Metadata is saved even when optional artwork fails.
            onArtwork(ArtworkChoices())
            if (keys.steamGridDb.isNotBlank()) {
                try {
                    onArtwork(catalog.artwork(match.title, keys.steamGridDb))
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    selectedError = "Game details loaded, but artwork is unavailable: ${readableCatalogError(error)}"
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            selectedError = "Could not load game details: ${readableCatalogError(error)}"
        } finally {
            loadingDetails = false
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("1  ·  FIND A GAME", fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFDCE9F8), fontSize = 11.sp, letterSpacing = 1.3.sp)
        Text("Search for a game and tap the correct result. We'll fill the details for you.",
            color = Color(0xFFABB9D2), fontSize = 12.sp, lineHeight = 18.sp)
        if (keys.rawg.isBlank()) {
            Text("Add a free RAWG API key in Settings to enable search. Manual entry still works.",
                color = Color(0xFFFFCB8C), fontSize = 13.sp)
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    selected = null
                    selectedError = null
                },
                label = { Text("Search games · RAWG") },
                placeholder = { Text("e.g. Elden Ring, Control, Pragmata") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = { if (loadingSearch || loadingDetails) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
            )
            if (matches.isNotEmpty() && selected == null) {
                Surface(color = Color(0xFF232C40), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        matches.forEach { match ->
                            Row(Modifier.fillMaxWidth().clickable {
                                selected = match
                                query = match.title
                                matches = emptyList()
                            }.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (match.thumbnailUrl.startsWith("https://")) {
                                    AsyncImage(match.thumbnailUrl, null,
                                        Modifier.size(54.dp, 62.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(match.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    if (match.releaseDate.isNotBlank()) Text(match.releaseDate, color = Color(0xFFABB9D2), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else if (searchCompleted && !loadingSearch && query.trim().length >= 2 && selected == null && searchError == null) {
                Text("No matches. You can enter the game manually below.", color = Color(0xFFABB9D2), fontSize = 12.sp)
            }
            searchError?.let { Text(it, color = Color(0xFFFFA9AC), fontSize = 12.sp) }
            selectedError?.let {
                Text(it, color = Color(0xFFFFC58A), fontSize = 12.sp)
                if (selected != null) TextButton(onClick = { retrySerial++ }) { Text("Retry details") }
            }
            if (selected != null) {
                Text("✓ ${selected?.title} selected · Game details will fill automatically",
                    color = Color(0xFF98E6B6), fontSize = 12.sp)
            } else if (currentRawgId != null) {
                Text("✓ Game details linked to RAWG. Search to choose another game.",
                    color = Color(0xFF98E6B6), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ArtworkOptionRow(
    label: String,
    choices: List<ArtworkOption>,
    selectedUrl: String,
    onSelect: (String) -> Unit,
    portrait: Boolean
) {
    if (choices.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(choices, key = { it.url }) { choice ->
                Surface(
                    modifier = Modifier.width(if (portrait) 90.dp else 160.dp)
                        .height(if (portrait) 135.dp else 90.dp).clickable { onSelect(choice.url) },
                    shape = RoundedCornerShape(11.dp),
                    border = if (choice.url == selectedUrl) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF75D8FF)) else null,
                    color = Color(0xFF273248)
                ) {
                    AsyncImage(choice.thumbnail, "Choose $label artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

private fun readableCatalogError(error: Exception): String = when (error) {
    is retrofit2.HttpException -> when (error.code()) {
        401, 403 -> "API key rejected. Check Settings."
        429 -> "API rate limit reached. Try again later."
        else -> "Provider returned HTTP ${error.code()}."
    }
    is java.io.IOException -> "No connection to game database."
    else -> error.message ?: "Unknown API error."
}
