package com.gamevault.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamevault.app.data.catalog.CatalogKeys
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes
import kotlinx.coroutines.launch

/** First-login on-device setup. Explicit buttons only: no accidental dismissal. */
@Composable
fun ApiKeyOnboardingDialog(
    currentKeys: CatalogKeys,
    onSave: suspend (String, String) -> Unit,
    onLater: suspend () -> Unit,
    onComplete: () -> Unit
) {
    val palette = VaultPalettes.pc
    val scope = rememberCoroutineScope()
    var rawgKey by rememberSaveable { mutableStateOf(currentKeys.rawg) }
    var steamKey by rememberSaveable { mutableStateOf(currentKeys.steamGridDb) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun commit(task: suspend () -> Unit) {
        if (working) return
        scope.launch {
            working = true
            error = null
            try {
                task()
                onComplete()
            } catch (failure: Exception) {
                error = failure.message ?: "Couldn't save your preference. Please try again."
            } finally {
                working = false
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val shape = RoundedCornerShape(26.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF202D46), Color(0xFF101827), Color(0xFF0C111E))))
                .border(1.dp, Color.White.copy(alpha = .16f), shape)
                .padding(horizontal = 21.dp, vertical = 19.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VaultBrand(iconColor = palette.accent)
            Text(
                "Make your vault smarter.",
                color = Color.White, fontSize = 27.sp, lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Unlock instant game details and beautiful artwork. Add two free-tier API keys for the best GameVault experience.",
                color = VaultMuted, fontSize = 13.sp, lineHeight = 19.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BenefitPill("Search & details", palette.accent)
                BenefitPill("Album art", palette.accent)
            }
            HorizontalDivider(color = Color.White.copy(alpha = .10f))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ApiKeyEditorFields(
                    rawgKey = rawgKey,
                    onRawgKeyChange = { rawgKey = it },
                    steamKey = steamKey,
                    onSteamKeyChange = { steamKey = it },
                    accent = palette.accent
                )
                Text(
                    "You can always add or change these in Settings → Game Search & Artwork.",
                    color = VaultMuted, fontSize = 12.sp, lineHeight = 17.sp
                )
            }
            error?.let { Text(it, color = Color(0xFFFFB0BB), fontSize = 12.sp, lineHeight = 17.sp) }
            Button(
                onClick = { commit { onSave(rawgKey.trim(), steamKey.trim()) } },
                enabled = !working && (rawgKey.isNotBlank() || steamKey.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF0B1020))
            ) {
                if (working) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Save & Continue", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { commit { onLater() } },
                enabled = !working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("I'll Do This Later", color = Color(0xFFCDD5E4), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun BenefitPill(label: String, accent: Color) {
    Text(
        label,
        color = accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = .10f))
            .border(1.dp, accent.copy(alpha = .25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp)
    )
}
