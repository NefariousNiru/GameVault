package com.gamevault.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.gamevault.app.data.catalog.CatalogKeys
import com.gamevault.app.domain.catalog.ArtworkChoices
import com.gamevault.app.domain.catalog.GameCatalogRepository
import com.gamevault.app.domain.catalog.CatalogMetadata
import com.gamevault.app.domain.catalog.applyCatalog
import com.gamevault.app.ui.catalog.GameCatalogSearch
import com.gamevault.app.ui.catalog.ArtworkOptionRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.domain.game.*
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/** RAWG-first editor; manual entry and personal lifecycle control remain always available. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditorSheet(
    game: Game?,
    keys: CatalogKeys,
    catalog: GameCatalogRepository,
    defaultPlatform: GamePlatform,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (GameDraft) -> Unit
) {
    var draft by remember(game?.id) { mutableStateOf(game?.draft ?: GameDraft(platform = defaultPlatform)) }
    val accent = VaultPalettes.forPlatform(draft.platform).accent
    val validation = draft.validationError()
    var choices by remember(game?.id) { mutableStateOf(ArtworkChoices()) }
    var showAdvanced by remember(game?.id) { mutableStateOf(false) }
    var showCatalogFields by remember(game?.id) { mutableStateOf(false) }
    var fallbackCover by remember(game?.id) { mutableStateOf("") }
    var fallbackHero by remember(game?.id) { mutableStateOf("") }
    var userCoverChanged by remember(game?.id) { mutableStateOf(false) }
    var userHeroChanged by remember(game?.id) { mutableStateOf(false) }

    fun applyMetadata(metadata: CatalogMetadata) {
        // NEVER infer/change interest, acquisition, gameplay, notes, personal rating, or queue.
        // Existing artwork survives a refresh of the same game; a new game uses RAWG fallback.
        userCoverChanged = false
        userHeroChanged = false
        fallbackCover = metadata.fallbackImage
        fallbackHero = metadata.fallbackHero
        draft = draft.applyCatalog(metadata)
    }
    fun applyArtwork(artwork: ArtworkChoices) {
        choices = artwork
        if (!artwork.exactMatch) return
        draft = draft.copy(
            steamGridId = artwork.steamGridId,
            coverUrl = if (!userCoverChanged && (draft.coverUrl.isBlank() || draft.coverUrl == fallbackCover))
                artwork.covers.firstOrNull()?.url ?: draft.coverUrl else draft.coverUrl,
            heroUrl = if (!userHeroChanged && (draft.heroUrl.isBlank() || draft.heroUrl == fallbackHero))
                artwork.heroes.firstOrNull()?.url ?: draft.heroUrl else draft.heroUrl
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = true,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color(0xFF121725),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.87f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 35.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (game == null) "ADD A GAME" else "EDIT GAME", color = accent,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(if (game == null) "Add a game" else "Edit game",
                        fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            item {
                GameCatalogSearch(
                    keys = keys, catalog = catalog, currentRawgId = draft.rawgId,
                    onMetadata = ::applyMetadata, onArtwork = ::applyArtwork
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TITLE", color = accent, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { if (draft.rawgId == null) draft = draft.copy(title = it) },
                        readOnly = draft.rawgId != null,
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(15.dp),
                        supportingText = {
                            if (draft.rawgId != null) Text("Selected from search")
                        }
                    )
                }
            }
            item {
                Text("PLATFORM", fontSize = 11.sp, color = VaultMuted, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GamePlatform.entries.forEach { platform ->
                        val selected = draft.platform == platform
                        Button(
                            onClick = { draft = draft.copy(platform = platform) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) VaultPalettes.forPlatform(platform).accent else Color(0xFF262D3C),
                                contentColor = if (selected) Color(0xFF0B1020) else Color.White
                            )
                        ) { Text(platform.label, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item { HorizontalDivider(color = Color.White.copy(alpha = 0.10f)) }
            item {
                OutlinedButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAdvanced) "Hide your details ↑" else "Your game details ↓")
                }
            }
            if (showAdvanced) {
            item { Text("YOUR GAME STATUS", color = accent, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item { EnumPicker("Release status", draft.release, ReleaseStatus.entries, { it.label }) { draft = draft.copy(release = it) } }
            item { EnumPicker("Want to play?", draft.interest, InterestStatus.entries, { it.label }) { draft = draft.copy(interest = it) } }
            item { EnumPicker("Play status", draft.gameplay, GameplayStatus.entries, { it.label }) { draft = draft.copy(gameplay = it) } }
            if (draft.platform == GamePlatform.PC) {
                item { EnumPicker("PC download status", draft.pcAcquisition, PcAcquisition.entries, { it.label }) { draft = draft.copy(pcAcquisition = it) } }
            } else {
                item {
                    EnumPicker("How do you access this game?", draft.ps5Acquisition, Ps5Acquisition.entries, { it.label }) {
                        // Keep the legacy PS5 library field consistent with the chosen
                        // source; it remains stored for backward compatibility only.
                        draft = draft.copy(
                            ps5Acquisition = it,
                            ps5Library = if (it == Ps5Acquisition.NOT_OWNED || it == Ps5Acquisition.PREORDERED)
                                Ps5Library.NOT_ADDED else Ps5Library.IN_LIBRARY
                        )
                    }
                }
            }
            item { HorizontalDivider(color = Color.White.copy(alpha = 0.10f)) }
            item { Text("PERSONAL DETAILS", color = accent, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item { EnumPicker("Your rating", draft.rating, listOf<Int?>(null) + (1..10).map { it }, { it?.let { "$it / 10" } ?: "Unrated" }) { draft = draft.copy(rating = it) } }
            item { EditorText("Completion date (YYYY-MM-DD)", draft.completionDate) { draft = draft.copy(completionDate = it) } }
            item { OutlinedTextField(draft.notes, { draft = draft.copy(notes = it) }, label = { Text("Personal notes") }, minLines = 3, maxLines = 7, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) }
            } // personal statuses/details
            if (draft.rawgId == null) item {
                OutlinedButton(onClick = { showCatalogFields = !showCatalogFields }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showCatalogFields) "Hide game information ↑" else "Game information ↓")
                }
            }
            if (showCatalogFields && draft.rawgId == null) {
                item { Text("OPTIONAL DETAILS", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                item { EditorText("Developer", draft.developer) { draft = draft.copy(developer = it) } }
                item { EditorText("Publisher", draft.publisher) { draft = draft.copy(publisher = it) } }
                item { EditorText("Genres (comma-separated)", draft.genres) { draft = draft.copy(genres = it) } }
                item { EditorText("Tags", draft.tags) { draft = draft.copy(tags = it) } }
                item { EditorText("Supported platforms", draft.supportedPlatforms) { draft = draft.copy(supportedPlatforms = it) } }
                item { EditorText("Release date (YYYY-MM-DD)", draft.releaseDate) { draft = draft.copy(releaseDate = it) } }
                item { OutlinedTextField(draft.description, { draft = draft.copy(description = it) }, label = { Text("Game description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8) }
            }
            item { Text("ARTWORK", color = accent, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item {
                ArtworkOptionRow("Portrait covers", choices.covers, draft.coverUrl,
                    { userCoverChanged = true; draft = draft.copy(coverUrl = it) }, portrait = true)
            }
            item {
                ArtworkOptionRow("Background artwork", choices.heroes, draft.heroUrl,
                    { userHeroChanged = true; draft = draft.copy(heroUrl = it) }, portrait = false)
            }
            if (choices.matchedName.isNotBlank() && !choices.exactMatch) {
                item { Text("Artwork match was not exact; select manual URLs to avoid incorrect game art.", color = Color(0xFFFFCC90), fontSize = 12.sp) }
            }
            if (keys.steamGridDb.isBlank()) {
                item { Text("SteamGridDB key not configured. Add it in Settings for cover and hero choices.", color = VaultMuted, fontSize = 12.sp) }
            }
            item { EditorText("HTTPS cover image URL", draft.coverUrl) { userCoverChanged = true; draft = draft.copy(coverUrl = it) } }
            item { EditorText("HTTPS background image URL", draft.heroUrl) { userHeroChanged = true; draft = draft.copy(heroUrl = it) } }

            if (error != null) item { Text(error, color = Color(0xFFFFA6A6), fontSize = 13.sp) }
            item {
                Button(
                    onClick = { onSave(draft) },
                    enabled = !saving && validation == null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0E18))
                ) {
                    if (saving) CircularProgressIndicator(color = Color(0xFF090D1B))
                    else Text(if (game == null) "Add to library" else "Save changes", fontWeight = FontWeight.Bold)
                }
                if (validation != null && draft.title.isNotBlank()) Text(validation, color = VaultMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EditorText(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange,
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        singleLine = true, shape = RoundedCornerShape(15.dp))
}

@Composable
private fun <T> EnumPicker(
    label: String,
    selected: T,
    options: List<T>,
    textFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = VaultMuted, fontSize = 12.sp)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(textFor(selected), modifier = Modifier.weight(1f), color = Color.White)
            Text("⌄", color = VaultMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(textFor(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
