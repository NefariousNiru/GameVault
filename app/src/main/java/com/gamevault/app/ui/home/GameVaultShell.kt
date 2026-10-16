package com.gamevault.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gamevault.app.platform.NetworkStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gamevault.app.GameVaultApplication
import com.gamevault.app.data.catalog.CatalogKeys
import com.gamevault.app.domain.auth.AuthenticatedUser
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.components.VaultBackdrop
import com.gamevault.app.ui.game.GameDetailScreen
import com.gamevault.app.ui.game.GameEditorSheet
import com.gamevault.app.ui.game.GameLibraryViewModel
import com.gamevault.app.ui.library.LibraryScreen
import com.gamevault.app.ui.queue.QueueScreen
import com.gamevault.app.ui.settings.SettingsScreen
import com.gamevault.app.ui.settings.ApiKeyOnboardingDialog
import com.gamevault.app.ui.theme.SectionPalette
import com.gamevault.app.ui.theme.VaultPalettes
import kotlinx.coroutines.launch

/** Authenticated navigation shell and transient editor/detail surfaces. */
@Composable
fun GameVaultShell(
    user: AuthenticatedUser,
    busy: Boolean,
    onSignOut: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as GameVaultApplication).container
    val gamesViewModel: GameLibraryViewModel = viewModel(factory = GameLibraryViewModel.Factory(container.gameRepository))
    val state by gamesViewModel.state.collectAsStateWithLifecycle()
    // Wait for DataStore's real value before opening the setup dialog so
    // upgrading users see their existing device-local keys prefilled.
    val loadedKeysFlow = androidx.compose.runtime.remember(container) {
        container.apiKeySettings.keys.map<CatalogKeys, CatalogKeys?> { it }
    }
    val loadedKeys by loadedKeysFlow.collectAsStateWithLifecycle(initialValue = null)
    val apiKeys = loadedKeys ?: CatalogKeys()
    val keysScope = rememberCoroutineScope()
    var apiSettingsError by rememberSaveable { mutableStateOf<String?>(null) }
    var apiSettingsSaving by rememberSaveable { mutableStateOf(false) }
    var apiSavedCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(user.uid) { gamesViewModel.observe(user.uid) }
    val context = LocalContext.current
    LaunchedEffect(user.uid) {
        combine(
            NetworkStatus.observe(context.applicationContext),
            gamesViewModel.state.map { it.games }.distinctUntilChanged { old, new ->
                fun urls(items: List<com.gamevault.app.domain.game.Game>) = items
                    .flatMap { listOf(it.draft.coverUrl, it.draft.heroUrl) }
                    .filter { it.isNotBlank() }.distinct().sorted()
                urls(old) == urls(new)
            }
        ) { online, currentGames -> online to currentGames }
            .collectLatest { (online, currentGames) ->
                if (online && currentGames.isNotEmpty()) container.artworkPrefetcher.prefetch(currentGames)
            }
    }


    // Device-install-scoped: persists across sign-out and app restarts,
    // but Android does not back up noBackupFilesDir, so reinstall shows it again.
    var showApiKeyWelcome by rememberSaveable {
        mutableStateOf(!container.apiKeyOnboardingStore.hasCompleted())
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: VaultDestination.LIBRARY.route
    var platform by rememberSaveable { mutableStateOf(GamePlatform.PC) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingGame = state.games.firstOrNull { it.id == editId }
    val detailsGame = state.games.firstOrNull { it.id == detailId }

    val palette = when (route) {
        VaultDestination.QUEUE.route -> VaultPalettes.queue
        VaultDestination.SETTINGS.route -> VaultPalettes.settings
        else -> VaultPalettes.forPlatform(platform)
    }
    val current = VaultDestination.entries.firstOrNull { it.route == route } ?: VaultDestination.LIBRARY

    if (detailId != null && detailsGame != null) {
        GameDetailScreen(
            game = detailsGame,
            busy = state.saving,
            onDismiss = { detailId = null },
            onEdit = {
                editId = detailsGame.id
                detailId = null
                gamesViewModel.clearError()
                showEditor = true
            },
            onDelete = { gamesViewModel.delete(detailsGame) { detailId = null } },
            onToggleQueue = {
                gamesViewModel.setQueued(detailsGame, detailsGame.draft.queueRank == null)
            },
            queueError = state.error,
            onQuickStatus = { field, value -> gamesViewModel.setQuickStatus(detailsGame, field, value) }
        )
    } else {
        VaultBackdrop(palette) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    VaultBottomNavigation(palette, current) { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            ) { insets ->
                NavHost(
                    navController = navController,
                    startDestination = VaultDestination.LIBRARY.route,
                    modifier = Modifier.padding(insets)
                ) {
                    composable(VaultDestination.LIBRARY.route) {
                        LibraryScreen(
                            platform = platform,
                            onPlatformChanged = { platform = it },
                            state = state,
                            onAdd = {
                                gamesViewModel.clearError()
                                editId = null
                                showEditor = true
                            },
                            onOpen = { game ->
                                gamesViewModel.clearError()
                                detailId = game.id
                            },
                            onQuickStatus = { game, field, value -> gamesViewModel.setQuickStatus(game, field, value) }
                        )
                    }
                    composable(VaultDestination.QUEUE.route) {
                        QueueScreen(
                            games = state.games,
                            queueOrder = state.queueOrder,
                            busy = state.saving,
                            error = state.error,
                            onOpen = { game -> gamesViewModel.clearError(); detailId = game.id },
                            onRemove = { game -> gamesViewModel.setQueued(game, false) },
                            onClearQueue = { gamesViewModel.clearQueue() },
                            onReorder = { ids, onResult -> gamesViewModel.reorderQueue(ids, onResult) }
                        )
                    }
                    composable(VaultDestination.SETTINGS.route) {
                        SettingsScreen(
                            user = user,
                            busy = busy,
                            onSignOut = onSignOut,
                            apiKeys = apiKeys,
                            apiSettingsError = apiSettingsError,
                            apiSettingsSaving = apiSettingsSaving,
                            apiSavedCount = apiSavedCount,
                            backupGames = state.games,
                            backupQueueOrder = state.queueOrder,
                            backupStore = container.backupStore,
                            artworkPrefetcher = container.artworkPrefetcher,
                            onSaveApiKeys = { rawg, steam ->
                                apiSettingsError = null
                                if (!apiSettingsSaving) keysScope.launch {
                                    apiSettingsSaving = true
                                    try {
                                        container.apiKeySettings.save(rawg, steam)
                                        apiSavedCount++
                                    } catch (failure: Exception) {
                                        apiSettingsError = failure.message ?: "Unable to save API settings"
                                    } finally {
                                        apiSettingsSaving = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showApiKeyWelcome && loadedKeys != null) {
        ApiKeyOnboardingDialog(
            currentKeys = apiKeys,
            onSave = { rawg, steam ->
                container.apiKeySettings.save(rawg, steam)
                container.apiKeyOnboardingStore.markCompleted()
            },
            onLater = { container.apiKeyOnboardingStore.markCompleted() },
            onComplete = { showApiKeyWelcome = false }
        )
    }

    if (showEditor) {
        GameEditorSheet(
            game = editingGame,
            keys = apiKeys,
            catalog = container.catalogRepository,
            defaultPlatform = platform,
            saving = state.saving,
            error = state.error,
            onDismiss = { if (!state.saving) { showEditor = false; editId = null } },
            onSave = { draft ->
                gamesViewModel.save(editingGame, draft) {
                    showEditor = false
                    editId = null
                }
            }
        )
    }
}

@Composable
private fun VaultBottomNavigation(
    palette: SectionPalette,
    selected: VaultDestination,
    onSelect: (VaultDestination) -> Unit
) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockBrush = Brush.verticalGradient(listOf(palette.dockTop, palette.dockBottom))

    // Do not draw a full-width rectangular dock behind this row. The old
    // background leaked through the transparent corners of the rounded shape,
    // especially on the PC, PS5 and Settings palettes. Clip the *only* dock
    // surface, including the navigation-bar inset, before painting its gradient.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(dockBrush)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp + navBottom),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        VaultDestination.entries.forEach { destination ->
                val active = destination == selected
                val tint = if (active) palette.accent else Color(0xFF90A0B3)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (active) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onSelect(destination) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                    if (active) {
                        Spacer(Modifier.size(7.dp))
                        Text(
                            destination.label,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                    }
                }
        }
    }
}
