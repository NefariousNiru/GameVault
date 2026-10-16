package com.gamevault.app.ui.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamevault.app.data.backup.FirestoreBackupStore
import com.gamevault.app.domain.backup.BackupCodec
import com.gamevault.app.domain.backup.ImportChoice
import com.gamevault.app.domain.backup.ImportMode
import com.gamevault.app.domain.backup.LibraryBackup
import com.gamevault.app.domain.backup.conflicts
import com.gamevault.app.domain.game.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Android document picker: no storage permissions, no backup files uploaded to a server. */
@Composable
fun BackupSettingsSection(
    uid: String,
    games: List<Game>,
    queueOrder: List<String>,
    store: FirestoreBackupStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<LibraryBackup?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val choices = remember { mutableStateMapOf<String, ImportChoice>() }
    val cloud = remember(games, queueOrder) {
        runCatching { BackupCodec.export(games, queueOrder) }.getOrNull()
    }

    fun report(text: String) {
        message = text
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    var exportContent by remember { mutableStateOf<String?>(null) }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = exportContent
        exportContent = null
        if (uri != null && content != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write(content)
                    } ?: error("Cannot write to selected destination")
                }
                report("Backup exported")
            } catch (failure: Exception) { report("Export failed: ${failure.message}") }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            working = true
            try {
                // Restrict untrusted input before JSON parsing to prevent large allocations.
                val text = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val sink = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(sink.size() + count <= 5_000_000) { "Backup exceeds 5 MB" }
                            sink.write(buffer, 0, count)
                        }
                        sink.toByteArray()
                    } ?: error("Cannot read selected file")
                    require(bytes.size <= 5_000_000) { "Backup exceeds 5 MB" }
                    bytes.toString(Charsets.UTF_8)
                }
                preview = BackupCodec.decode(text)
                choices.clear()
                message = null
            } catch (failure: Exception) { report("Import rejected: ${failure.message}") }
            finally { working = false }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("BACKUP & RESTORE", color = Color(0xFFA2DFD8), fontWeight = FontWeight.Bold)
        Text("Save your collection as a JSON file, or restore it on another phone.", color = Color(0xFFC8D0DA))
        Button(
            onClick = {
                try {
                    exportContent = BackupCodec.encode(requireNotNull(cloud))
                    exportPicker.launch("GameVault-${System.currentTimeMillis()}.json")
                } catch (failure: Exception) { report("Cannot export: ${failure.message}") }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !working && cloud != null
        ) { Text("Export Library") }
        OutlinedButton(
            onClick = { importPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !working
        ) { Text("Import Backup") }
        OutlinedButton(
            onClick = {
                val backup = store.latestRecovery()
                if (backup == null) report("No recovery backup on this device")
                else {
                    exportContent = backup
                    exportPicker.launch("GameVault-recovery.json")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !working
        ) { Text("Export Recovery Copy") }
        TextButton(
            onClick = {
                scope.launch {
                    working = true
                    try { store.undoLastRestore(uid); report("Previous library restored") }
                    catch (failure: Exception) { report("Undo unavailable: ${failure.message}") }
                    finally { working = false }
                }
            },
            enabled = !working
        ) { Text("Undo Last Import") }
        message?.let { Text(it, color = Color(0xFFBDD8E5)) }
    }

    val imported = preview
    if (imported != null) {
        val conflicts = cloud?.let { conflicts(it, imported) }.orEmpty()
        var mode by remember(imported) { mutableStateOf(ImportMode.MERGE) }
        AlertDialog(
            onDismissRequest = { if (!working) preview = null },
            title = { Text("Import ${imported.games.size} games?") },
            text = {
                Column(
                    Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("The entire file was validated. Choose what to keep.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ImportMode.KEEP_CLOUD, ImportMode.MERGE, ImportMode.REPLACE).forEach { option ->
                            OutlinedButton(onClick = { mode = option }) {
                                Text(
                                    when (option) {
                                        ImportMode.KEEP_CLOUD -> "Keep Cloud"
                                        ImportMode.MERGE -> "Merge"
                                        ImportMode.REPLACE -> "Replace"
                                    },
                                    color = if (mode == option) Color(0xFF81D9FF) else Color.Unspecified
                                )
                            }
                        }
                    }
                    if (mode == ImportMode.REPLACE) {
                        Text("Your current library will be retained as a recoverable snapshot.", color = Color(0xFFFFCB8C))
                    }
                    if (mode == ImportMode.MERGE && conflicts.isNotEmpty()) {
                        Text("${conflicts.size} duplicate games. Choose which record to keep:")
                        conflicts.forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.existingTitle, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(ImportChoice.KEEP_CLOUD, ImportChoice.USE_BACKUP).forEach { choice ->
                                        OutlinedButton(onClick = { choices[item.key] = choice }) {
                                            Text(
                                                if (choice == ImportChoice.KEEP_CLOUD) "Cloud" else "Backup",
                                                color = if ((choices[item.key] ?: ImportChoice.KEEP_CLOUD) == choice)
                                                    Color(0xFF81D9FF) else Color.Unspecified
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (working) Text("Validating and staging restore…")
                }
            },
            confirmButton = {
                TextButton(enabled = !working, onClick = {
                    if (mode == ImportMode.KEEP_CLOUD) { preview = null; return@TextButton }
                    scope.launch {
                        working = true
                        try {
                            val result = store.restore(uid, imported, mode, choices.toMap())
                            preview = null
                            report(result)
                        } catch (failure: Exception) {
                            report("Import failed. Current library unchanged: ${failure.message}")
                        } finally { working = false }
                    }
                }) { Text(if (mode == ImportMode.KEEP_CLOUD) "Keep Cloud" else "Confirm Import") }
            },
            dismissButton = { TextButton(enabled = !working, onClick = { preview = null }) { Text("Cancel") } }
        )
    }
}
