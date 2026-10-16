package com.gamevault.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.gamevault.app.data.catalog.CatalogKeys
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
    onSaveApiKeys: (String, String) -> Unit
) {
    val palette = VaultPalettes.settings
    var rawgKey by remember(apiKeys.rawg) { mutableStateOf(apiKeys.rawg) }
    var steamKey by remember(apiKeys.steamGridDb) { mutableStateOf(apiKeys.steamGridDb) }
    var revealKeys by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { VaultBrand(iconColor = palette.accent) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YOUR SPACE", color = palette.accent, fontSize = 10.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.ExtraBold)
                Text("Settings", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                Text("Your account, your library, your control.", color = VaultMuted, fontSize = 14.sp)
            }
        }
        item {
            GlowCard(palette) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ACCOUNT", color = palette.accent, fontSize = 10.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(user.displayName ?: "Google account", color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    user.email?.let { Text(it, color = VaultMuted, fontSize = 13.sp) }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text("Private collection · Firebase Authentication", color = VaultMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("GAME DATABASE APIS", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Provide personal API keys once on this device. They are stored only in private app preferences, not Firebase or the source code.",
                        color = VaultMuted, fontSize = 12.sp)
                    OutlinedTextField(rawgKey, { rawgKey = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("RAWG API key · required for search") }, singleLine = true,
                        visualTransformation = if (revealKeys) VisualTransformation.None else PasswordVisualTransformation())
                    OutlinedTextField(steamKey, { steamKey = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("SteamGridDB API key · optional artwork") }, singleLine = true,
                        visualTransformation = if (revealKeys) VisualTransformation.None else PasswordVisualTransformation())
                    TextButton(onClick = { revealKeys = !revealKeys }) {
                        Text(if (revealKeys) "Hide keys" else "Show keys")
                    }
                    Button(onClick = { onSaveApiKeys(rawgKey, steamKey) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save API credentials")
                    }
                    apiSettingsError?.let { Text(it, color = Color(0xFFFFA6A6), fontSize = 12.sp) }
                    Text("RAWG: rawg.io/apidocs  ·  SteamGridDB: steamgriddb.com/profile/preferences/api",
                        color = VaultMuted, fontSize = 11.sp)
                }
            }
        }
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painterResource(R.drawable.gv_ic_cloud), null,
                            tint = palette.accent, modifier = Modifier.size(22.dp))
                        Text("Cloud connection", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text("Run a real Firestore write and server read to verify your Firebase setup.",
                        color = VaultMuted, fontSize = 13.sp, lineHeight = 20.sp)
                    Button(
                        onClick = onVerifyCloud, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent,
                            contentColor = Color(0xFF101624))
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                        else Text("Test Firestore connection", fontWeight = FontWeight.Bold)
                    }
                    message?.let { Text(it, color = Color(0xFF8FE0BD), fontSize = 12.sp) }
                    error?.let { Text(it, color = Color(0xFFFFA8A8), fontSize = 12.sp) }
                }
            }
        }
        item {
            GlowCard(palette) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("COMING LATER", color = palette.accent, fontSize = 10.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Offline artwork · Backup & restore", color = Color.White, fontSize = 14.sp)
                    Text("These features will arrive in Phase 7 after game records and artwork are live.",
                        color = VaultMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.gv_ic_logout), null, Modifier.size(19.dp))
                Spacer(Modifier.size(8.dp))
                Text("Sign out")
            }
        }
        item { Text("GAMEVAULT · PHASE 4", color = VaultMuted, fontSize = 10.sp, letterSpacing = 2.sp) }
    }
}
