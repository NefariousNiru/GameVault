package com.gamevault.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamevault.app.ui.library.LibraryDisplayPreferences
import com.gamevault.app.ui.library.LocalDisplayPreferences
import com.gamevault.app.ui.library.restoredLibraryFilter
import com.gamevault.app.ui.library.restoredLibraryPlatform
import com.gamevault.app.ui.library.restoredLibraryRoute
import com.gamevault.app.ui.library.restoredLibrarySort
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.localDisplayStore by preferencesDataStore(name = "gamevault_local_display")

/**
 * Persistent DEVICE-ONLY view preferences. Each Google account has its own
 * PC/PS5 UI state on this phone, with no Firestore writes or JSON export.
 * Android backup explicitly excludes this DataStore file.
 */
class LocalDisplayPreferencesStore(context: Context) {
    private val store = context.applicationContext.localDisplayStore

    // Account IDs are not written as plain DataStore keys; this prefix also keeps
    // display choices separate when friends use different accounts on one device.
    private fun prefix(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) } + "_"
    }

    private fun stringKey(prefix: String, field: String) = stringPreferencesKey(prefix + field)
    private fun boolKey(prefix: String, field: String) = booleanPreferencesKey(prefix + field)

    private fun readLibrary(prefs: Preferences, id: String, platform: String): LibraryDisplayPreferences {
        val scope = id + platform + "_"
        return LibraryDisplayPreferences(
            query = prefs[stringKey(scope, "search")].orEmpty(),
            grid = prefs[boolKey(scope, "grid")] ?: true,
            filter = restoredLibraryFilter(prefs[stringKey(scope, "filter")]),
            sort = restoredLibrarySort(prefs[stringKey(scope, "sort")])
        )
    }

    fun settings(uid: String): Flow<LocalDisplayPreferences> {
        val id = prefix(uid)
        return store.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }.map { prefs ->
            LocalDisplayPreferences(
                platform = restoredLibraryPlatform(prefs[stringKey(id, "platform")]),
                route = restoredLibraryRoute(prefs[stringKey(id, "route")]),
                pc = readLibrary(prefs, id, "pc"),
                ps5 = readLibrary(prefs, id, "ps5")
            )
        }
    }

    suspend fun save(uid: String, display: LocalDisplayPreferences) {
        val id = prefix(uid)
        store.edit { prefs ->
            prefs[stringKey(id, "platform")] = display.platform.name
            prefs[stringKey(id, "route")] = display.route
            fun saveLibrary(platform: String, value: LibraryDisplayPreferences) {
                val scope = id + platform + "_"
                prefs[stringKey(scope, "search")] = value.query
                prefs[boolKey(scope, "grid")] = value.grid
                prefs[stringKey(scope, "filter")] = restoredLibraryFilter(value.filter)
                prefs[stringKey(scope, "sort")] = value.sort.name
            }
            saveLibrary("pc", display.pc)
            saveLibrary("ps5", display.ps5)
        }
    }
}
