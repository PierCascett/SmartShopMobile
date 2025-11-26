package it.unito.smartshopmobile.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites_datastore")

/**
 * Simple persistence for the customer's favorite product ids.
 * Stored per-user to avoid mixing lists between accounts.
 */
class FavoritesDataStore(private val context: Context) {
    private val gson = Gson()

    private fun keyForUser(userId: Int) = stringPreferencesKey("favorites_$userId")

    fun favoritesFlow(userId: Int): Flow<Set<String>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[keyForUser(userId)]?.let { raw ->
                gson.fromJson<Set<String>>(raw, object : TypeToken<Set<String>>() {}.type)
            } ?: emptySet()
        }

    suspend fun saveFavorites(userId: Int, favorites: Set<String>) {
        context.favoritesDataStore.edit { prefs ->
            prefs[keyForUser(userId)] = gson.toJson(favorites)
        }
    }
}
