package com.gamevault.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.components.GlowCard
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.game.GameLibraryUiState
import com.gamevault.app.ui.theme.SectionPalette
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/** Real Firestore-driven collection, with no fabricated sample games. */
@Composable
fun LibraryScreen(
    platform: GamePlatform,
    onPlatformChanged: (GamePlatform) -> Unit,
    state: GameLibraryUiState,
    onAdd: () -> Unit,
    onOpen: (Game) -> Unit
) {
    val palette = VaultPalettes.forPlatform(platform)
    var query by rememberSaveable { mutableStateOf("") }
    var viewGrid by rememberSaveable { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var sortNewFirst by rememberSaveable { mutableStateOf(false) }
    val visibleGames = state.games.filter { game ->
        game.draft.platform == platform && game.draft.title.contains(query.trim(), ignoreCase = true) && when (filter) {
            "Playing" -> game.draft.gameplay == GameplayStatus.PLAYING
            "Completed" -> game.draft.gameplay == GameplayStatus.COMPLETED
            "Wishlist" -> game.draft.interest.name == "WISHLIST"
            else -> true
        }
    }.let { list -> if (sortNewFirst) list.sortedByDescending { it.updatedAtMillis } else list.sortedBy { it.draft.title.lowercase() } }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THE COLLECTION", color = palette.accent, letterSpacing = 3.sp, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Your games. Your universe.", fontSize = 29.sp, lineHeight = 35.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text("One home for every adventure.", color = VaultMuted, fontSize = 13.sp)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { PlatformSwitcher(platform, onPlatformChanged) }
        item(span = { GridItemSpan(maxLineSpan) }) { LibraryHero(platform, palette) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Your collection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(if (platform == GamePlatform.PC) "PC titles" else "PlayStation 5 titles", color = VaultMuted, fontSize = 12.sp)
                }
                Text("$libraryCount GAMES", color = palette.accent, fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Button(
                onClick = onAdd, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF0C1020))
            ) { Text("＋  Add a game", fontWeight = FontWeight.ExtraBold) }
        }
        if (state.games.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search your library") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("All", "Playing", "Completed", "Wishlist").forEach { option ->
                        FilterChip(
                            selected = filter == option, onClick = { filter = option },
                            label = { Text(option, fontSize = 10.sp) }
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = viewGrid, onClick = { viewGrid = true }, label = { Text("Grid") })
                        FilterChip(selected = !viewGrid, onClick = { viewGrid = false }, label = { Text("List") })
                    }
                    FilterChip(selected = sortNewFirst, onClick = { sortNewFirst = !sortNewFirst }, label = { Text(if (sortNewFirst) "Recent ↓" else "A–Z") })
                }
            }
        }
        if (state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("Loading your collection…", color = VaultMuted) }
        } else if (visibleGames.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlowCard(palette) {
                    Column(Modifier.fillMaxWidth().padding(27.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(R.drawable.gv_ic_library), null, tint = palette.accent, modifier = Modifier.size(39.dp))
                        Text(if (libraryCount == 0) "Make your first entry" else "No matches found", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (libraryCount == 0) "Tap Add a game to build your personal collection." else "Try a different search or filter.", color = VaultMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(visibleGames, key = { it.id }) { game ->
                GameLibraryCard(game = game, palette = palette, grid = viewGrid, onClick = { onOpen(game) })
            }
        }
    }
}

@Composable
private fun GameLibraryCard(game: Game, palette: SectionPalette, grid: Boolean, onClick: () -> Unit) {
    val draft = game.draft
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(Color(0xFF1B2334))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onClick).padding(if (grid) 0.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(if (grid) Modifier.fillMaxWidth() else Modifier) {
            Box(
                modifier = if (grid) Modifier.fillMaxWidth().aspectRatio(.76f) else Modifier.size(width = 77.dp, height = 104.dp).clip(shape)
            ) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.glow.copy(alpha = .5f), Color(0xFF121726)))))
                if (draft.coverUrl.isNotBlank()) AsyncImage(
                    model = draft.coverUrl, contentDescription = "${draft.title} cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                ) else Icon(painterResource(R.drawable.gv_ic_library), null, Modifier.align(Alignment.Center).size(46.dp), tint = palette.accent)
                if (grid) Text(draft.platform.label, modifier = Modifier.align(Alignment.TopEnd).padding(9.dp).background(Color(0xDD121522), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, fontSize = 10.sp)
            }
            if (grid) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(draft.title, maxLines = 2, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(draft.gameplay.label, color = palette.accent, fontSize = 11.sp)
                }
            }
        }
        if (!grid) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(draft.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${draft.platform.label} · ${draft.gameplay.label}", color = palette.accent, fontSize = 12.sp)
                Text(draft.interest.label, color = VaultMuted, fontSize = 11.sp)
            }
            Text("›", modifier = Modifier.padding(end = 11.dp), color = palette.accent, fontSize = 25.sp)
        }
    }
}

@Composable
private fun PlatformSwitcher(selected: GamePlatform, onSelect: (GamePlatform) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF171C2B)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GamePlatform.entries.forEach { platform ->
            val active = platform == selected
            val color = VaultPalettes.forPlatform(platform).accent
            Row(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) color.copy(alpha = .16f) else Color.Transparent)
                    .border(BorderStroke(1.dp, if (active) color.copy(alpha = .4f) else Color.Transparent), RoundedCornerShape(14.dp))
                    .clickable { onSelect(platform) }.padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(if (platform == GamePlatform.PC) R.drawable.gv_ic_pc else R.drawable.gv_ic_ps5), null, Modifier.size(19.dp), tint = if (active) color else VaultMuted)
                Spacer(Modifier.size(8.dp))
                Text(platform.label, color = if (active) Color.White else VaultMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibraryHero(platform: GamePlatform, palette: SectionPalette) {
    Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(25.dp))
        .border(BorderStroke(1.dp, palette.accent.copy(alpha = .18f)), RoundedCornerShape(25.dp))) {
        Image(painterResource(if (platform == GamePlatform.PC) R.drawable.gv_hero_pc else R.drawable.gv_hero_ps5), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE0080D19), Color(0x9B080D19), Color.Transparent))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(palette.label, color = palette.accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(if (platform == GamePlatform.PC) "Every world,\none place." else "Play beyond\nthe ordinary.", color = Color.White, fontSize = 26.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your collection, your way", color = Color(0xFFE1E5F2), fontSize = 12.sp)
        }
    }
}
