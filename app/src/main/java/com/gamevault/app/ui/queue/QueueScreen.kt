package com.gamevault.app.ui.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gamevault.app.R
import com.gamevault.app.domain.game.Game
import com.gamevault.app.ui.components.GlowCard
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/** One shared queue; membership is explicit and independent of all lifecycle statuses. */
@Composable
fun QueueScreen(
    games: List<Game>,
    busy: Boolean,
    error: String?,
    onOpen: (Game) -> Unit,
    onRemove: (Game) -> Unit
) {
    val palette = VaultPalettes.queue
    val queued = games.filter { it.draft.queueRank != null }
        .sortedWith(compareBy<Game> { it.draft.queueRank }.thenBy { it.id })

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { VaultBrand(iconColor = palette.accent) }
        item { Text("Play Queue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 31.sp) }
        if (error != null) {
            item { Text(error, color = Color(0xFFFFB4B4), fontSize = 12.sp) }
        }
        if (queued.isEmpty()) {
            item {
                GlowCard(palette) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(painterResource(R.drawable.gv_ic_queue), null, tint = palette.accent, modifier = Modifier.size(44.dp))
                        Text("Your queue is empty", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Open a game and choose Add to Queue.", color = VaultMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            itemsIndexed(queued, key = { _, game -> game.id }) { index, game ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC24161A))
                        .clickable { onOpen(game) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${index + 1}", color = palette.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (game.draft.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = game.draft.coverUrl,
                            contentDescription = "${game.draft.title} album art",
                            modifier = Modifier.size(width = 52.dp, height = 70.dp).clip(RoundedCornerShape(9.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(game.draft.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(game.draft.platform.label, color = VaultMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { onRemove(game) }, enabled = !busy) {
                        Text("Remove", color = palette.accent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
