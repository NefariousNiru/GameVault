package com.gamevault.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.ui.theme.VaultMuted

/** Shared RAWG + SteamGridDB fields for both Settings and first-login setup. */
@Composable
fun ApiKeyEditorFields(
    rawgKey: String,
    onRawgKeyChange: (String) -> Unit,
    steamKey: String,
    onSteamKeyChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    showPrivacyNote: Boolean = true
) {
    val context = LocalContext.current
    var revealKeys by remember { mutableStateOf(false) }
    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Can't open the link", Toast.LENGTH_SHORT).show()
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProviderHelp("Game search & details", "RAWG", "Get a free-tier RAWG API key ↗") {
            openLink("https://rawg.io/apidocs")
        }
        OutlinedTextField(
            value = rawgKey, onValueChange = onRawgKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("RAWG API key") }, singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (revealKeys) VisualTransformation.None else PasswordVisualTransformation()
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.09f))
        ProviderHelp("Game artwork", "SteamGridDB", "Get a free-tier SteamGridDB API key ↗") {
            openLink("https://www.steamgriddb.com/profile/preferences/api")
        }
        OutlinedTextField(
            value = steamKey, onValueChange = onSteamKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SteamGridDB API key") }, singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (revealKeys) VisualTransformation.None else PasswordVisualTransformation()
        )
        TextButton(onClick = { revealKeys = !revealKeys }) {
            Text(if (revealKeys) "Hide keys" else "Show keys", color = accent)
        }
        if (showPrivacyNote) {
            Text(
                "Keys are saved only on this device, not in your cloud library or JSON backups.",
                color = VaultMuted, fontSize = 12.sp, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ProviderHelp(title: String, service: String, action: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(service, color = VaultMuted, fontSize = 12.sp)
        TextButton(onClick = onClick, modifier = Modifier.padding(start = 0.dp)) {
            Text(action, fontSize = 12.sp)
        }
    }
}
