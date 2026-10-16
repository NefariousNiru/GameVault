package com.gamevault.app.ui.game

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

/** Full-screen game experience: cover photo + album art with a gradient sampled from the portrait album art. */
@Composable
fun GameDetailScreen(
    game: Game,
    busy: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleQueue: () -> Unit,
    queueError: String? = null
) {
    BackHandler(onBack = onDismiss)
    val draft = game.draft
    val palette = VaultPalettes.forPlatform(draft.platform)
    val context = LocalContext.current
    var confirmDelete by remember(game.id) { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${draft.title}?") },
            text = { Text("This game will be removed from your collection.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { confirmDelete = false; onDelete() }) {
                    Text("Remove", color = Color(0xFFFFAEB7))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    GameArtworkBackdrop(
        artworkUrl = draft.coverUrl,
        fallback = palette,
        modifier = Modifier.fillMaxSize()
    ) { mood ->
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("‹  Back", color = Color.White, fontSize = 17.sp) }
                Spacer(Modifier.weight(1f))
                Text(
                    "GAME DETAILS",
                    color = mood.accent,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 19.dp, end = 19.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp)
            ) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(182.dp).clip(RoundedCornerShape(22.dp))
                            .background(Brush.horizontalGradient(listOf(mood.top, mood.middle)))
                    ) {
                        if (draft.heroUrl.isNotBlank()) {
                            AsyncImage(
                                model = draft.heroUrl,
                                contentDescription = "${draft.title} cover image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color(0xC9080D16)))
                            )
                        )
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(width = 138.dp, height = 196.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(mood.middle)
                        ) {
                            if (draft.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = draft.coverUrl,
                                    contentDescription = "${draft.title} album art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    "GAMEVAULT",
                                    color = mood.accent,
                                    modifier = Modifier.align(Alignment.Center),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(
                                draft.platform.label.uppercase(),
                                color = mood.accent,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                draft.title,
                                color = Color.White,
                                fontSize = 25.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                draft.gameplay.label,
                                color = Color(0xFFCDDCE8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                item {
                    GameInfoPanel("AT A GLANCE", mood) {
                        InfoRow("Release", draft.releaseDate.ifBlank { draft.release.label })
                        if (draft.developer.isNotBlank()) InfoRow("Developer", draft.developer)
                        if (draft.genres.isNotBlank()) InfoRow("Genre", draft.genres)
                        if (draft.rawgRating != null) InfoRow("Community Rating", "${draft.rawgRating} / 5")
                    }
                }
                item {
                    GameInfoPanel("YOUR GAME", mood) {
                        InfoRow("Play Status", draft.gameplay.label)
                        InfoRow("Interest", draft.interest.label)
                        InfoRow("Your Rating", draft.rating?.let { "$it / 10" } ?: "Not rated")
                        InfoRow(
                            if (draft.platform == GamePlatform.PC) "Download Status" else "How You Access It",
                            if (draft.platform == GamePlatform.PC) draft.pcAcquisition.label else draft.ps5Acquisition.label
                        )
                        if (draft.completionDate.isNotBlank()) InfoRow("Completed", draft.completionDate)
                        if (draft.notes.isNotBlank()) InfoRow("Notes", draft.notes)
                        InfoRow("Play Queue", if (draft.queueRank == null) "Not queued" else "Queued")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onToggleQueue,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(49.dp),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, mood.accent.copy(alpha = 0.55f))
                    ) {
                        Text(
                            if (draft.queueRank == null) "+ Add to Queue" else "Remove from Queue",
                            color = mood.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    queueError?.let { Text(it, color = Color(0xFFFFAAAA), fontSize = 12.sp) }
                }
                item {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mood.accent,
                            contentColor = Color(0xFF08121E)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Edit Game Details", fontWeight = FontWeight.Bold)
                    }
                }
                if (draft.description.isNotBlank()) {
                    item {
                        GameInfoPanel("ABOUT THE GAME", mood) {
                            Text(
                                draft.description,
                                color = Color(0xFFE3E9F1),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                item {
                    GameInfoPanel("CREDITS & SOURCES", mood) {
                        if (draft.publisher.isNotBlank()) InfoRow("Publisher", draft.publisher)
                        if (draft.supportedPlatforms.isNotBlank()) InfoRow("Platforms", draft.supportedPlatforms)
                        if (draft.tags.isNotBlank()) InfoRow("Tags", draft.tags)
                        val sources = buildList {
                            if (draft.rawgId != null) add("RAWG")
                            if (draft.steamGridId != null) add("SteamGridDB")
                        }.joinToString()
                        if (sources.isNotBlank()) InfoRow("Available on", sources)
                        if (draft.rawgId != null) {
                            TextButton(onClick = {
                                val url = "https://rawg.io/games/${Uri.encode(draft.rawgSlug.ifBlank { draft.rawgId.toString() })}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) { Text("Open RAWG ↗", color = mood.accent) }
                        }
                        if (draft.steamGridId != null) {
                            TextButton(onClick = {
                                val url = "https://www.steamgriddb.com/game/${draft.steamGridId}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) { Text("Open SteamGridDB ↗", color = mood.accent) }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(49.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("Remove from Library", color = Color(0xFFFFB7BE))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameInfoPanel(title: String, mood: ArtworkMood, content: @Composable () -> Unit) {
    Surface(
        color = Color(0xCA101724),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(title, color = mood.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = VaultMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
