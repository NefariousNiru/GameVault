package com.gamevault.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.gamevault.app.ui.theme.SectionPalette
import com.gamevault.app.ui.theme.VaultPalettes
import kotlinx.coroutines.launch

/** Authenticated navigation shell and transient editor/detail surfaces. */
@Composable
fun GameVaultShell(
    user: AuthenticatedUser,
    busy: Boolean,
    message: String?,
    error: String?,
    onVerifyCloud: () -> Unit,
    onSignOut: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as GameVaultApplication).container
    val gamesViewModel: GameLibraryViewModel = viewModel(factory = GameLibraryViewModel.Factory(container.gameRepository))
    val state by gamesViewModel.state.collectAsStateWithLifecycle()
    val apiKeys by container.apiKeySettings.keys.collectAsStateWithLifecycle(initialValue = CatalogKeys())
    val keysScope = rememberCoroutineScope()
    var apiSettingsError by rememberSaveable { mutableStateOf<String?>(null) }
    var apiSettingsSaving by rememberSaveable { mutableStateOf(false) }
    var apiSavedCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(user.uid) { gamesViewModel.observe(user.uid) }

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
            onDelete = { gamesViewModel.delete(detailsGame) { detailId = null } }
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
                            }
                        )
                    }
                    composable(VaultDestination.QUEUE.route) { QueueScreen(state.games) }
                    composable(VaultDestination.SETTINGS.route) {
                        SettingsScreen(
                            user = user,
                            busy = busy,
                            message = message,
                            error = error,
                            onVerifyCloud = onVerifyCloud,
                            onSignOut = onSignOut,
                            apiKeys = apiKeys,
                            apiSettingsError = apiSettingsError,
                            apiSettingsSaving = apiSettingsSaving,
                            apiSavedCount = apiSavedCount,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(dockBrush)
    ) {
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
}
