package com.gamevault.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.R
import com.gamevault.app.data.catalog.CatalogKeys
import com.gamevault.app.domain.auth.AuthenticatedUser
import com.gamevault.app.ui.components.GlowCard
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

@Composable
fun SettingsScreen(
    user: AuthenticatedUser,
    busy: Boolean,
    message: String?,
    error: String?,
    onVerifyCloud: () -> Unit,
    onSignOut: () -> Unit,
    apiKeys: CatalogKeys,
    apiSettingsError: String?,
    apiSettingsSaving: Boolean,
    apiSavedCount: Int,
    onSaveApiKeys: (String, String) -> Unit
) {
    val palette = VaultPalettes.settings
    val context = LocalContext.current
    var rawgKey by remember(apiKeys.rawg) { mutableStateOf(apiKeys.rawg) }
    var steamKey by remember(apiKeys.steamGridDb) { mutableStateOf(apiKeys.steamGridDb) }
    var revealKeys by remember { mutableStateOf(false) }
    val dirty = rawgKey.trim() != apiKeys.rawg || steamKey.trim() != apiKeys.steamGridDb

    // Display once per successful write, even if the stored settings Flow emits again.
    LaunchedEffect(apiSavedCount) {
        if (apiSavedCount > 0) {
            Toast.makeText(context, "API keys saved on this device", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 20.dp, bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { VaultBrand(iconColor = palette.accent) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", color = Color.White, fontSize = 38.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ACCOUNT", color = palette.accent, fontSize = 11.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(user.displayName ?: "Google account", color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    user.email?.let { Text(it, color = VaultMuted, fontSize = 13.sp) }
                }
            }
        }
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GAME SEARCH & ARTWORK", color = palette.accent,
                        fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                    Text("Add your own API keys to unlock game search and artwork choices.",
                        color = VaultMuted, fontSize = 13.sp, lineHeight = 19.sp)

                    ApiProviderLink("Search games", "RAWG", "Get a RAWG API key ↗") {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://rawg.io/apidocs")))
                    }
                    OutlinedTextField(
                        value = rawgKey, onValueChange = { rawgKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("RAWG API key") }, singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (revealKeys) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = .09f))
                    ApiProviderLink("Display game artwork", "SteamGridDB", "Get a SteamGridDB API key ↗") {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.steamgriddb.com/profile/preferences/api")))
                    }
                    OutlinedTextField(
                        value = steamKey, onValueChange = { steamKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("SteamGridDB API key") }, singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (revealKeys) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    TextButton(onClick = { revealKeys = !revealKeys }) {
                        Text(if (revealKeys) "Hide keys" else "Show keys", color = palette.accent)
                    }
                    Button(
                        onClick = { onSaveApiKeys(rawgKey.trim(), steamKey.trim()) },
                        enabled = dirty && !apiSettingsSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (apiSettingsSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else Text(if (dirty) "Save API keys" else "API keys up to date", fontWeight = FontWeight.Bold)
                    }
                    apiSettingsError?.let { Text(it, color = Color(0xFFFFB4B4), fontSize = 12.sp) }
                    if (apiSavedCount > 0 && !dirty && apiSettingsError == null) {
                        Text("✓ Saved successfully on this device", color = Color(0xFF9BEAC0), fontSize = 12.sp)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .09f))
                    Text(
                        "Your API keys stay on this device and aren't stored by us in the cloud. " +
                            "They won't sync to other devices or be restored after reinstalling the app.",
                        color = VaultMuted, fontSize = 12.sp, lineHeight = 19.sp
                    )
                }
            }
        }
        // Temporary developer diagnostic. Remove this card and related code before release.
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("CONNECTION CHECK · DEVELOPMENT", color = palette.accent,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Check that your cloud library can connect.", color = VaultMuted, fontSize = 13.sp)
                    Button(onClick = onVerifyCloud, enabled = !busy,
                        modifier = Modifier.fillMaxWidth()) {
                        if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                        else Text("Test cloud connection")
                    }
                    message?.let { Text(it, color = Color(0xFF9BEAC0), fontSize = 12.sp) }
                    error?.let { Text(it, color = Color(0xFFFFA8A8), fontSize = 12.sp) }
                }
            }
        }
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("COMING LATER", color = palette.accent, fontSize = 10.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Offline artwork · Backup & restore", color = Color.White, fontSize = 14.sp)
                    Text("These arrive in Phase 7.", color = VaultMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = onSignOut, enabled = !busy,
                modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.gv_ic_logout), null, Modifier.size(19.dp))
                Spacer(Modifier.size(8.dp))
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun ApiProviderLink(title: String, provider: String, link: String, onLink: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(provider, color = VaultMuted, fontSize = 12.sp)
        TextButton(onClick = onLink, modifier = Modifier.padding(start = 0.dp)) {
            Text(link, fontSize = 12.sp)
        }
    }
}
