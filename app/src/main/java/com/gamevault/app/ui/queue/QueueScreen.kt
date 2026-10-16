package com.gamevault.app.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.gamevault.app.R
import com.gamevault.app.domain.game.Game
import com.gamevault.app.ui.components.GlowCard
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Optimistic drag-and-drop queue. The finger moves the lifted row, neighbors
 * animate into place and a single Firestore reorder occurs on release.
 * Incoming snapshots never replace a local drag or an unacknowledged order.
 */
@Composable
fun QueueScreen(
    games: List<Game>,
    queueOrder: List<String>,
    busy: Boolean,
    error: String?,
    onOpen: (Game) -> Unit,
    onRemove: (Game) -> Unit,
    onClearQueue: () -> Unit,
    onReorder: (List<String>, (Boolean) -> Unit) -> Unit
) {
    val palette = VaultPalettes.queue
    val queued = games.filter { it.draft.queueRank != null }
        .sortedWith(compareBy<Game> {
            queueOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
        }.thenBy { it.draft.queueRank }.thenBy { it.id })
    val remoteIds = queued.map { it.id }
    val byId = queued.associateBy { it.id }
    val localIds = remember { mutableStateListOf<String>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var awaitingEcho by remember { mutableStateOf<List<String>?>(null) }
    var committing by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var removeCandidate by remember { mutableStateOf<Game?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { 106.dp.toPx() } // 92dp row + 14dp gap
    val latestRemote by rememberUpdatedState(remoteIds)
    val latestOnReorder by rememberUpdatedState(onReorder)
    val locked = busy || committing || awaitingEcho != null || draggingId != null

    // Only reconcile a server snapshot when there is no local drag/commit to display.
    LaunchedEffect(remoteIds, draggingId, awaitingEcho) {
        if (draggingId != null) return@LaunchedEffect
        val pending = awaitingEcho
        if (pending != null) {
            if (remoteIds == pending) {
                awaitingEcho = null
            } else if (pending.toSet() == remoteIds.toSet()) {
                return@LaunchedEffect // Snapshot still contains pre-commit order.
            } else {
                // Membership changed on another device. Its new order wins.
                awaitingEcho = null
            }
        }
        if (localIds.toList() != remoteIds) {
            localIds.clear()
            localIds.addAll(remoteIds)
        }
    }

    fun endDrag(commit: Boolean) {
        if (draggingId == null) return
        draggingId = null
        dragOffset = 0f
        if (!commit) {
            localIds.clear()
            localIds.addAll(latestRemote)
            return
        }
        val desired = localIds.toList()
        if (desired == latestRemote) return
        awaitingEcho = desired
        committing = true
        latestOnReorder(desired) { success ->
            committing = false
            if (!success) {
                awaitingEcho = null
                localIds.clear()
                localIds.addAll(latestRemote)
            }
        }
    }

    if (removeCandidate != null) {
        val candidate = removeCandidate!!
        AlertDialog(
            onDismissRequest = { removeCandidate = null },
            title = { Text("Remove from Queue?") },
            text = { Text("Remove ${candidate.draft.title} from your play queue? It will remain in your library.") },
            confirmButton = {
                TextButton(onClick = { removeCandidate = null; onRemove(candidate) }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removeCandidate = null }) { Text("Cancel") } }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear Play Queue?") },
            text = { Text("Remove all ${queued.size} games from the queue? Your library and game statuses will stay unchanged.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearQueue() }) { Text("Clear All") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "brand") { VaultBrand(iconColor = palette.accent) }
        item(key = "heading") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Play Queue", color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 31.sp, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuExpanded = true }, enabled = queued.isNotEmpty() && !locked) {
                        Text("⋮", color = palette.accent, fontSize = 27.sp)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Clear All") },
                            onClick = { menuExpanded = false; confirmClear = true }
                        )
                    }
                }
            }
        }
        if (error != null) item(key = "error") {
            Text(error, color = Color(0xFFFFB4B4), fontSize = 12.sp)
        }
        if (queued.isEmpty()) {
            item(key = "empty") {
                GlowCard(palette) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(R.drawable.gv_ic_queue), null, tint = palette.accent,
                            modifier = Modifier.size(44.dp))
                        Text("Your queue is empty", color = Color.White, fontSize = 19.sp,
                            fontWeight = FontWeight.Bold)
                        Text("Open a game and choose Add to Queue.", color = VaultMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            item(key = "tip") { Text("Hold ☷ to reorder", color = VaultMuted, fontSize = 12.sp) }
            itemsIndexed(localIds, key = { _, id -> id }) { index, id ->
                val game = byId[id] ?: return@itemsIndexed
                val active = draggingId == id
                Row(
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                        .zIndex(if (active) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (active) dragOffset else 0f
                            shadowElevation = if (active) 14.dp.toPx() else 0f
                        }
                        .then(if (active) Modifier else Modifier.animateItem())
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) Color(0xFF45322F) else Color(0xCC24161A))
                        .clickable(enabled = !locked) { onOpen(game) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("${index + 1}", color = palette.accent, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold)
                    if (game.draft.coverUrl.isNotBlank()) {
                        AsyncImage(game.draft.coverUrl, "${game.draft.title} album art",
                            Modifier.size(width = 46.dp, height = 64.dp)
                                .clip(RoundedCornerShape(9.dp)),
                            contentScale = ContentScale.Crop)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(game.draft.title, color = Color.White,
                            fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(game.draft.platform.label, color = VaultMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { removeCandidate = game }, enabled = !locked) {
                        Text("×", color = palette.accent, fontSize = 19.sp)
                    }
                    Text("☷", color = if (active) Color.White else palette.accent,
                        fontSize = 29.sp,
                        modifier = Modifier.size(width = 34.dp, height = 54.dp)
                            .pointerInput(game.id, busy, committing, awaitingEcho != null) {
                                if (busy || committing || awaitingEcho != null) return@pointerInput
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = game.id
                                        dragOffset = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragCancel = { endDrag(false) },
                                    onDragEnd = { endDrag(true) },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        if (draggingId != game.id) return@detectDragGesturesAfterLongPress
                                        dragOffset += delta.y
                                        // Adjust translation after each swap so the row stays under the finger.
                                        while (abs(dragOffset) >= stepPx * .68f) {
                                            val current = localIds.indexOf(game.id)
                                            val direction = if (dragOffset > 0f) 1 else -1
                                            val target = current + direction
                                            if (target !in localIds.indices) {
                                                dragOffset = 0f
                                                break
                                            }
                                            localIds.add(target, localIds.removeAt(current))
                                            dragOffset -= direction * stepPx
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        // Scroll nearby list items when dragging near an edge.
                                        val activeItem = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == game.id }
                                        if (activeItem != null) {
                                            val center = activeItem.offset + activeItem.size / 2f + dragOffset
                                            val start = listState.layoutInfo.viewportStartOffset
                                            val end = listState.layoutInfo.viewportEndOffset
                                            val scroll = when {
                                                center > end - stepPx -> 18f
                                                center < start + stepPx -> -18f
                                                else -> 0f
                                            }
                                            if (scroll != 0f) coroutineScope.launch { listState.scrollBy(scroll) }
                                        }
                                    }
                                )
                            })
                }
            }
        }
    }
}
