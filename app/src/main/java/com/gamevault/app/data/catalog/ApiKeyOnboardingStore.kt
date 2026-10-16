package com.gamevault.app.data.catalog

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Device-install-scoped onboarding marker. noBackupFilesDir is deliberately
 * excluded from Android cloud backup and device-to-device restoration, so a
 * reinstall (even with restored preferences) shows onboarding again.
 * This marker is never synced to Firebase and is never part of a JSON export.
 */
class ApiKeyOnboardingStore internal constructor(private val marker: File) {
    constructor(context: Context) : this(
        File(context.applicationContext.noBackupFilesDir, "api_keys_intro_seen_v1")
    )

    fun hasCompleted(): Boolean = marker.exists()

    suspend fun markCompleted() = withContext(Dispatchers.IO) {
        if (!marker.exists()) marker.writeText("1")
    }
}
