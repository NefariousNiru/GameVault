package com.gamevault.app.ui.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.components.GameArtworkBackdrop
import com.gamevault.app.ui.theme.ArtworkMood
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/**
 * Artwork is a restrained full-sheet backdrop, not a profile-cover banner.
 * Important external facts lead, followed by your personal state, other game
 * information, controls, tags, and creator/source credit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(
    game: Game,
    busy: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val draft = game.draft
    val palette = VaultPalettes.forPlatform(draft.platform)
    val context = LocalContext.current
    var confirmDelete by remember(game.id) { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${draft.title}?") },
            text = { Text("This removes the game from your synced collection. This cannot be undone until backups are available.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { confirmDelete = false; onDelete() }) {
                    Text("Remove", color = Color(0xFFFF999F))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF080C13),
        contentColor = Color.White,
        dragHandle = null
    ) {
        GameArtworkBackdrop(
            heroUrl = draft.heroUrl,
            fallback = palette,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.93f)
        ) { mood ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, bottom = 38.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Let the selected background art breathe without putting text on it.
                item { Spacer(Modifier.height(132.dp)) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        if (draft.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = draft.coverUrl,
                                contentDescription = "${draft.title} cover art",
                                modifier = Modifier.size(98.dp, 136.dp).clip(RoundedCornerShape(15.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(draft.platform.label.uppercase(), color = mood.accent,
                                letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(draft.title, color = Color.White, fontSize = 26.sp,
                                lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
                            GameTag(draft.gameplay.label, mood.accent)
                        }
                    }
                }
                item {
                    DetailPanel("AT A GLANCE", mood) {
                        InfoPair("Release", draft.releaseDate.ifBlank { draft.release.label })
                        if (draft.genres.isNotBlank()) InfoPair("Genre", draft.genres)
                        if (draft.rawgRating != null) {
                            InfoPair("RAWG community rating", "${draft.rawgRating} / 5")
                        }
                        InfoPair("Your rating", draft.rating?.let { "$it / 10" } ?: "Not rated yet")
                    }
                }
                item {
                    DetailPanel("YOUR GAME", mood) {
                        InfoPair("Play status", draft.gameplay.label)
                        InfoPair("Interest", draft.interest.label)
                        InfoPair("Release status", draft.release.label)
                        if (draft.completionDate.isNotBlank()) InfoPair("Finished on", draft.completionDate)
                        if (draft.notes.isNotBlank()) InfoPair("Your notes", draft.notes)
                    }
                }
                if (draft.description.isNotBlank()) {
                    item {
                        DetailPanel("ABOUT THE GAME", mood) {
                            Text(draft.description, color = Color(0xFFE6EAF0),
                                fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
                item {
                    DetailPanel("HOW YOU HAVE IT", mood) {
                        if (draft.platform == GamePlatform.PC) {
                            InfoPair("PC download status", draft.pcAcquisition.label)
                        } else {
                            InfoPair("PlayStation access", draft.ps5Acquisition.label)
                            // Legacy ps5Library is preserved for old data, but no longer
                            // presented as a duplicate purchase/library control.
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onEdit,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = mood.accent, contentColor = Color(0xFF081017)
                            )
                        ) { Text("Edit game details", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { confirmDelete = true }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Remove game", color = Color(0xFFFFB5B9)) }
                    }
                }
                if (draft.tags.isNotBlank()) {
                    item {
                        DetailPanel("TAGS", mood) {
                            Text(draft.tags, color = Color(0xFFE1E5EC), fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
                item {
                    DetailPanel("CREDITS & SOURCES", mood) {
                        if (draft.developer.isNotBlank()) InfoPair("Developer", draft.developer)
                        if (draft.publisher.isNotBlank()) InfoPair("Publisher", draft.publisher)
                        if (draft.supportedPlatforms.isNotBlank()) InfoPair("Available on", draft.supportedPlatforms)
                        if (draft.rawgId != null) {
                            TextButton(onClick = {
                                val url = "https://rawg.io/games/${Uri.encode(draft.rawgSlug.ifBlank { draft.rawgId.toString() })}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) { Text("Game details supplied by RAWG  ↗", color = mood.accent) }
                        }
                        if (draft.steamGridId != null) {
                            TextButton(onClick = {
                                val url = "https://www.steamgriddb.com/game/${draft.steamGridId}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) { Text("Artwork from SteamGridDB  ↗", color = mood.accent) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPanel(title: String, mood: ArtworkMood, content: @Composable () -> Unit) {
    Surface(
        color = Color(0xEC101720),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(title, color = mood.accent, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, letterSpacing = 1.7.sp)
            content()
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = VaultMuted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun GameTag(text: String, tint: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = .17f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
