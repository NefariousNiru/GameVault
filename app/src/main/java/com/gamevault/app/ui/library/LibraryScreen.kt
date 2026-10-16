package com.gamevault.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gamevault.app.R
import com.gamevault.app.domain.game.Game
import com.gamevault.app.domain.game.GameplayStatus
import com.gamevault.app.domain.game.QuickStatusField
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.components.GlowCard
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.game.GameLibraryUiState
import com.gamevault.app.ui.theme.SectionPalette
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

@Composable
fun LibraryScreen(
    platform: GamePlatform,
    onPlatformChanged: (GamePlatform) -> Unit,
    state: GameLibraryUiState,
    preferences: LibraryDisplayPreferences,
    onPreferencesChange: (LibraryDisplayPreferences) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Game) -> Unit,
    onQuickStatus: (Game, QuickStatusField, String) -> Unit
) {
    val palette = VaultPalettes.forPlatform(platform)
    val query = preferences.query
    val viewGrid = preferences.grid
    val filter = preferences.filter
    val sortMode = preferences.sort
    var sortExpanded by rememberSaveable { mutableStateOf(false) }

    val visibleGames = state.games.filter { game ->
        game.draft.platform == platform &&
            game.draft.title.contains(query.trim(), ignoreCase = true) &&
            when (filter) {
                "Playing" -> game.draft.gameplay == GameplayStatus.PLAYING
                "Completed" -> game.draft.gameplay == GameplayStatus.COMPLETED
                "Wishlist" -> game.draft.interest.name == "WISHLIST"
                else -> true
            }
    }.let { sortedGames(it, sortMode) }
    val libraryCount = state.games.count { it.draft.platform == platform }

    LazyVerticalGrid(
        columns = if (viewGrid) GridCells.Fixed(2) else GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { VaultBrand(iconColor = palette.accent) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "THE COLLECTION",
                fontSize = 19.sp,
                lineHeight = 24.sp,
                color = palette.accent,
                fontWeight = FontWeight.ExtraBold
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) { PlatformSwitcher(platform, onPlatformChanged) }
        item(span = { GridItemSpan(maxLineSpan) }) { LibraryHero(platform, palette) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Games", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        if (platform == GamePlatform.PC) "PC titles" else "PlayStation 5 titles",
                        color = VaultMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    "$libraryCount GAMES",
                    color = palette.accent,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFF0C1020)
                )
            ) { Text("＋  Add a game", fontWeight = FontWeight.ExtraBold) }
        }
        if (state.games.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { onPreferencesChange(preferences.copy(query = it)) },
                    leadingIcon = { Text("⌕", fontSize = 24.sp, color = palette.accent) },
                    placeholder = { Text("Search your games") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                        focusedContainerColor = Color(0xEE172234),
                        unfocusedContainerColor = Color(0xCC121C2B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = VaultMuted,
                        unfocusedPlaceholderColor = VaultMuted
                    )
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Playing", "Completed", "Wishlist").forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { onPreferencesChange(preferences.copy(filter = option)) },
                            label = { Text(option, fontSize = 12.sp) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF172131),
                                labelColor = Color(0xFFD7E0EE),
                                selectedContainerColor = palette.accent.copy(alpha = .24f),
                                selectedLabelColor = Color.White
                            ),
                            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filter == option,
                                borderColor = Color.White.copy(alpha = .14f),
                                selectedBorderColor = palette.accent.copy(alpha = .56f)
                            )
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewGrid,
                            onClick = { onPreferencesChange(preferences.copy(grid = true)) },
                            label = { Text("Grid") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF172131),
                                labelColor = Color(0xFFD7E0EE),
                                selectedContainerColor = palette.accent.copy(alpha = .20f),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = !viewGrid,
                            onClick = { onPreferencesChange(preferences.copy(grid = false)) },
                            label = { Text("List") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF172131),
                                labelColor = Color(0xFFD7E0EE),
                                selectedContainerColor = palette.accent.copy(alpha = .20f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { sortExpanded = true },
                            label = { Text(sortMode.label, fontSize = 12.sp) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent.copy(alpha = .18f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF172131),
                                labelColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            containerColor = Color(0xFF1A2435)
                        ) {
                            GameSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, color = Color.White) },
                                    onClick = { onPreferencesChange(preferences.copy(sort = option)); sortExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("Loading your collection…", color = VaultMuted) }
        } else if (visibleGames.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlowCard(palette) {
                    Column(
                        Modifier.fillMaxWidth().padding(27.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.gv_ic_library),
                            null,
                            tint = palette.accent,
                            modifier = Modifier.size(39.dp)
                        )
                        Text(
                            if (libraryCount == 0) "Make your first entry" else "No matches found",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (libraryCount == 0) "Tap Add a game to build your collection." else "Try a different search or filter.",
                            color = VaultMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(visibleGames, key = { it.id }) { game ->
                GameLibraryCard(
                    game = game, palette = palette, grid = viewGrid,
                    onClick = { onOpen(game) },
                    onPlayStatus = { next -> onQuickStatus(game, QuickStatusField.PLAY_STATUS, next) }
                )
            }
        }
    }
}

@Composable
private fun GameLibraryCard(
    game: Game,
    palette: SectionPalette,
    grid: Boolean,
    onClick: () -> Unit,
    onPlayStatus: (String) -> Unit
) {
    var expanded by androidx.compose.runtime.remember(game.id) { mutableStateOf(false) }
    val draft = game.draft
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(Color(0xCC182133))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onClick).padding(if (grid) 0.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(if (grid) Modifier.fillMaxWidth() else Modifier) {
            Box(
                modifier = if (grid) Modifier.fillMaxWidth().aspectRatio(.76f) else Modifier.size(width = 77.dp, height = 104.dp).clip(shape)
            ) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.glow.copy(alpha = .55f), Color(0xFF121726)))))
                if (draft.coverUrl.isNotBlank()) AsyncImage(
                    model = draft.coverUrl,
                    contentDescription = "${draft.title} album art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                ) else Icon(painterResource(R.drawable.gv_ic_library), null, Modifier.align(Alignment.Center).size(46.dp), tint = palette.accent)
                if (grid) Text(
                    draft.platform.label,
                    modifier = Modifier.align(Alignment.TopEnd).padding(9.dp).background(Color(0xDD121522), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            if (grid) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(draft.title, maxLines = 2, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Box {
                        Text(
                            "${draft.gameplay.label}  ▾", color = palette.accent, fontSize = 11.sp,
                            modifier = Modifier.clickable { expanded = true }
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            GameplayStatus.entries.forEach { status ->
                                DropdownMenuItem(text = { Text(status.label) }, onClick = {
                                    expanded = false
                                    if (status != draft.gameplay) onPlayStatus(status.name)
                                })
                            }
                        }
                    }
                }
            }
        }
        if (!grid) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(draft.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Box {
                    Text("${draft.platform.label} · ${draft.gameplay.label} ▾", color = palette.accent,
                        fontSize = 12.sp, modifier = Modifier.clickable { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GameplayStatus.entries.forEach { status ->
                            DropdownMenuItem(text = { Text(status.label) }, onClick = {
                                expanded = false
                                if (status != draft.gameplay) onPlayStatus(status.name)
                            })
                        }
                    }
                }
                Text(draft.interest.label, color = VaultMuted, fontSize = 11.sp)
            }
            Text("›", modifier = Modifier.padding(end = 11.dp), color = palette.accent, fontSize = 25.sp)
        }
    }
}

@Composable
private fun PlatformSwitcher(selected: GamePlatform, onSelect: (GamePlatform) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xCC171C2B)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GamePlatform.entries.forEach { platform ->
            val active = platform == selected
            val color = VaultPalettes.forPlatform(platform).accent
            Row(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) color.copy(alpha = .16f) else Color.Transparent)
                    .border(BorderStroke(1.dp, if (active) color.copy(alpha = .4f) else Color.Transparent), RoundedCornerShape(14.dp))
                    .clickable { onSelect(platform) }.padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(if (platform == GamePlatform.PC) R.drawable.gv_ic_pc else R.drawable.gv_ic_ps5),
                    null,
                    Modifier.size(19.dp),
                    tint = if (active) color else VaultMuted
                )
                Spacer(Modifier.size(8.dp))
                Text(platform.label, color = if (active) Color.White else VaultMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibraryHero(platform: GamePlatform, palette: SectionPalette) {
    val pc = platform == GamePlatform.PC
    val shape = RoundedCornerShape(25.dp)
    Box(
        Modifier.fillMaxWidth().height(194.dp).clip(shape)
            .background(palette.background)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = .18f)), shape)
    ) {
        Image(
            painter = painterResource(if (pc) R.drawable.gv_banner_pc else R.drawable.gv_banner_ps5),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = if (pc) Alignment.CenterStart else Alignment.CenterEnd
        )
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xD708101C), Color(0x9608101C), Color.Transparent))
        ))
        Column(
            Modifier.align(Alignment.BottomStart).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                if (pc) "PC LIBRARY" else "PS5 LIBRARY",
                color = palette.accent,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (pc) "The World of\nPC Gaming" else "Play Has\nNo Limits",
                color = Color.White,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
