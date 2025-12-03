/**
 * SessionDataStore.kt
 *
 * MVVM: Model Layer - Persistenza sessione utente (DataStore)
 *
 * FUNZIONAMENTO:
 * - Salva/carica utente loggato in DataStore Preferences
 * - Espone Flow<User?> per osservare sessione reattivamente
 * - Serializzazione JSON con Gson
 * - Alternativa moderna a SharedPreferences
 *
 * PATTERN MVVM:
 * - Data persistence: mantiene stato tra restart app
 * - Flow: stream reattivo, UI si aggiorna automaticamente
 * - suspend fun: operazioni asincrone (Coroutines)
 * - Usato da ViewModel per persistenza sessione
 *
 * USO TIPICO:
 * - Login: sessionDataStore.saveUser(user)
 * - Osserva: sessionDataStore.userFlow.collect { user -> ... }
 * - Logout: sessionDataStore.clear()
 */
package it.unito.smartshopmobile.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import it.unito.smartshopmobile.data.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_datastore")

/**
 * SessionDataStore - salva/ripristina l'utente loggato in DataStore.
 */
class SessionDataStore(private val context: Context) {
    private val KEY_USER_JSON = stringPreferencesKey("user_json")
    private val gson = Gson()

    val userFlow: Flow<User?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_JSON]?.let { json -> gson.fromJson(json, User::class.java) }
    }

    suspend fun saveUser(user: User) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_JSON] = gson.toJson(user)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_JSON)
        }
    }
}


