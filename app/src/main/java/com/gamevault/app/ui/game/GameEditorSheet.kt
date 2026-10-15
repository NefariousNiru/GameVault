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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

/** Manual entry is deliberate; RAWG search and metadata will be added in phase 4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditorSheet(
    game: Game?,
    defaultPlatform: GamePlatform,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (GameDraft) -> Unit
) {
    var draft by remember(game?.id) { mutableStateOf(game?.draft ?: GameDraft(platform = defaultPlatform)) }
    val accent = VaultPalettes.forPlatform(draft.platform).accent
    val validation = draft.validationError()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                    Text(if (game == null) "A new adventure" else "Curate this game",
                        fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Changes are private and saved to your GameVault cloud library.",
                        color = VaultMuted, fontSize = 12.sp)
                }
            }
            item {
                OutlinedTextField(
                    value = draft.title, onValueChange = { draft = draft.copy(title = it) },
                    label = { Text("Game title *") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(15.dp)
                )
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
            item { Text("LIFECYCLE", color = accent, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item { EnumPicker("Release", draft.release, ReleaseStatus.entries, { it.label }) { draft = draft.copy(release = it) } }
            item { EnumPicker("Interest", draft.interest, InterestStatus.entries, { it.label }) { draft = draft.copy(interest = it) } }
            item { EnumPicker("Gameplay", draft.gameplay, GameplayStatus.entries, { it.label }) { draft = draft.copy(gameplay = it) } }
            if (draft.platform == GamePlatform.PC) {
                item { EnumPicker("PC acquisition", draft.pcAcquisition, PcAcquisition.entries, { it.label }) { draft = draft.copy(pcAcquisition = it) } }
            } else {
                item { EnumPicker("PS5 acquisition", draft.ps5Acquisition, Ps5Acquisition.entries, { it.label }) { draft = draft.copy(ps5Acquisition = it) } }
                item { EnumPicker("PS5 library", draft.ps5Library, Ps5Library.entries, { it.label }) { draft = draft.copy(ps5Library = it) } }
            }
            item { HorizontalDivider(color = Color.White.copy(alpha = 0.10f)) }
            item { Text("PERSONAL DETAILS", color = accent, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item { EnumPicker("My rating", draft.rating, listOf<Int?>(null) + (1..10).map { it }, { it?.let { "$it / 10" } ?: "Unrated" }) { draft = draft.copy(rating = it) } }
            item { EditorText("Release date (YYYY-MM-DD)", draft.releaseDate) { draft = draft.copy(releaseDate = it) } }
            item { EditorText("Completion date (YYYY-MM-DD)", draft.completionDate) { draft = draft.copy(completionDate = it) } }
            item { OutlinedTextField(draft.notes, { draft = draft.copy(notes = it) }, label = { Text("Personal notes") }, minLines = 3, maxLines = 7, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) }
            item { Text("ARTWORK (OPTIONAL)", color = accent, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item { Text("Portrait cover and landscape hero links. Image search/selection comes in Phase 4.", color = VaultMuted, fontSize = 12.sp) }
            item { EditorText("HTTPS cover image URL", draft.coverUrl) { draft = draft.copy(coverUrl = it) } }
            item { EditorText("HTTPS hero image URL", draft.heroUrl) { draft = draft.copy(heroUrl = it) } }
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
