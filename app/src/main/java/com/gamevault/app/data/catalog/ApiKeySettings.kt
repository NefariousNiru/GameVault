package com.gamevault.app.data.catalog

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.catalogSettingsStore by preferencesDataStore(name = "catalog_credentials")

data class CatalogKeys(val rawg: String = "", val steamGridDb: String = "")

/** Device-local credentials. Never written to Firestore, exported, or logged. */
class ApiKeySettings(context: Context) {
    private val store = context.applicationContext.catalogSettingsStore
    private val rawg = stringPreferencesKey("rawg_key")
    private val steam = stringPreferencesKey("steamgriddb_key")
    val keys: Flow<CatalogKeys> = store.data.map { CatalogKeys(it[rawg].orEmpty(), it[steam].orEmpty()) }
    suspend fun save(rawgKey: String, steamKey: String) {
        store.edit { prefs ->
            prefs[rawg] = rawgKey.trim()
            prefs[steam] = steamKey.trim()
        }
    }
}
