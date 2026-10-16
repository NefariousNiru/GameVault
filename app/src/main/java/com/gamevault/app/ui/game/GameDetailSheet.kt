package com.gamevault.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/** Artwork-aware detail sheet; queue and metadata actions are added in stages 4/6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(game: Game, busy: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val draft = game.draft
    val context = LocalContext.current
    val palette = VaultPalettes.forPlatform(draft.platform)
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${draft.title}?") },
            text = { Text("This deletes the game from your synced collection. This cannot be undone until backups are implemented.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }, enabled = !busy) { Text("Delete", color = Color(0xFFFF8B94)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF121725), contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 35.dp)) {
            Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(12.dp))) {
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(palette.background, palette.glow))))
                if (draft.heroUrl.isNotBlank()) AsyncImage(
                    model = draft.heroUrl, contentDescription = "${draft.title} hero artwork",
                    modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop
                )
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB9121725)))))
                Text(draft.platform.label, Modifier.align(Alignment.BottomStart).padding(20.dp), color = palette.accent, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(draft.title, color = Color.White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameTag(draft.gameplay.label, palette.accent)
                    GameTag(draft.interest.label, Color(0xFFCFC8FF))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (draft.coverUrl.isNotBlank()) AsyncImage(draft.coverUrl, "${draft.title} cover", Modifier.size(width = 93.dp, height = 130.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        DetailText("Release", draft.release.label + if (draft.releaseDate.isNotBlank()) " · ${draft.releaseDate}" else "")
                        DetailText("Gameplay", draft.gameplay.label)
                        DetailText("Your rating", draft.rating?.let { "$it / 10" } ?: "Not rated")
                    }
                }
                if (draft.developer.isNotBlank()) DetailText("Developer", draft.developer)
                if (draft.publisher.isNotBlank()) DetailText("Publisher", draft.publisher)
                if (draft.genres.isNotBlank()) DetailText("Genres", draft.genres)
                if (draft.supportedPlatforms.isNotBlank()) DetailText("Supported platforms", draft.supportedPlatforms)
                if (draft.rawgRating != null) DetailText("RAWG community rating", "${draft.rawgRating} / 5")
                if (draft.description.isNotBlank()) DetailText("About the game", draft.description)
                if (draft.tags.isNotBlank()) DetailText("Tags", draft.tags)
                if (draft.rawgId != null) {
                    TextButton(onClick = {
                        val uri = Uri.parse("https://rawg.io/games/${Uri.encode(draft.rawgSlug.ifBlank { draft.rawgId.toString() })}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }) { Text("Game information supplied by RAWG · View on RAWG ↗") }
                }
                if (draft.steamGridId != null) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.steamgriddb.com/game/${draft.steamGridId}")))
                    }) { Text("Artwork: SteamGridDB ↗") }
                }
                DetailText("Acquisition", if (draft.platform == GamePlatform.PC) draft.pcAcquisition.label else "${draft.ps5Acquisition.label} · ${draft.ps5Library.label}")
                if (draft.completionDate.isNotBlank()) DetailText("Completed on", draft.completionDate)
                if (draft.notes.isNotBlank()) DetailText("Notes", draft.notes)
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF090D1B))) {
                    Text("Edit details and statuses", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove from library", color = Color(0xFFFFA2AB))
                }
            }
        }
    }
}

@Composable private fun DetailText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label.uppercase(), color = VaultMuted, fontSize = 10.sp, letterSpacing = 1.1.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}
@Composable private fun GameTag(text: String, tint: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = .13f)).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(text, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
