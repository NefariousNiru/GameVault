package com.gamevault.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.gamevault.app.data.catalog.CatalogKeys
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gamevault.app.GameVaultApplication
import com.gamevault.app.domain.auth.AuthenticatedUser
import com.gamevault.app.domain.model.GamePlatform
import com.gamevault.app.ui.components.VaultBackdrop
import com.gamevault.app.ui.game.GameDetailSheet
import com.gamevault.app.ui.game.GameEditorSheet
import com.gamevault.app.ui.game.GameLibraryViewModel
import com.gamevault.app.ui.library.LibraryScreen
import com.gamevault.app.ui.queue.QueueScreen
import com.gamevault.app.ui.settings.SettingsScreen
import com.gamevault.app.ui.theme.SectionPalette
import com.gamevault.app.ui.theme.VaultPalettes

/** Authenticated navigation and transient sheets. State persists in ViewModel + Firestore. */
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
                    SettingsScreen(user, busy, message, error, onVerifyCloud, onSignOut,
                        apiKeys = apiKeys, apiSettingsError = apiSettingsError,
                        apiSettingsSaving = apiSettingsSaving, apiSavedCount = apiSavedCount,
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
                        })
                }
            }
        }
    }

    if (detailId != null && detailsGame != null) {
        GameDetailSheet(
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
    NavigationBar(modifier = Modifier.fillMaxWidth(), containerColor = palette.background,
        contentColor = Color.White, tonalElevation = 0.dp) {
        VaultDestination.entries.forEach { destination ->
            val active = destination == selected
            val iconTint by animateColorAsState(
                targetValue = if (active) palette.accent else Color(0xFF8D97AA),
                label = "tab tint"
            )
            NavigationBarItem(
                selected = active,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(painterResource(destination.iconRes), contentDescription = destination.label,
                        tint = iconTint, modifier = Modifier.size(24.dp))
                },
                label = { Text(destination.label, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = Color.White,
                    indicatorColor = palette.accent.copy(alpha = .12f),
                    unselectedIconColor = Color(0xFF8D97AA),
                    unselectedTextColor = Color(0xFF8D97AA)
                )
            )
        }
    }
}
