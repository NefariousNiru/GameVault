package com.gamevault.app.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.R
import com.gamevault.app.domain.game.Game
import com.gamevault.app.ui.components.GlowCard
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

@Composable
fun QueueScreen(games: List<Game>) {
    val palette = VaultPalettes.queue
    val queued = games.filter { it.draft.queueRank != null }.sortedBy { it.draft.queueRank }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(21.dp)) {
        item { VaultBrand(iconColor = palette.accent) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Play Queue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 31.sp)
            }
        }
        if (queued.isEmpty()) {
            item {
                GlowCard(palette) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(painterResource(R.drawable.gv_ic_queue), null, tint = palette.accent, modifier = Modifier.size(48.dp))
                        Text("The adventure starts here", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Games you queue will appear here.",
                            color = VaultMuted, fontSize = 13.sp, lineHeight = 21.sp)
                    }
                }
            }
        } else {
            items(queued, key = { it.id }) { game ->
                Row(Modifier.fillMaxWidth().background(Color(0xCC24161A), RoundedCornerShape(17.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("${(game.draft.queueRank ?: 0L) + 1}", color = palette.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Column {
                        Text(game.draft.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(game.draft.platform.label, color = VaultMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
