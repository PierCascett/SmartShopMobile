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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import it.unito.smartshopmobile.data.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_datastore")

/**
 * Gestisce la persistenza della sessione utente tramite DataStore Preferences.
 *
 * Questa classe fornisce un'astrazione type-safe per salvare e ripristinare
 * l'utente autenticato utilizzando Jetpack DataStore come storage.
 * I dati vengono serializzati in JSON tramite Gson.
 *
 * Caratteristiche principali:
 * - Persistenza reattiva tramite Flow
 * - Operazioni asincrone con coroutine
 * - Alternativa moderna a SharedPreferences
 * - Automatic backup con DataStore
 *
 * Uso tipico:
 * ```kotlin
 * // Salva sessione
 * sessionDataStore.saveUser(user)
 *
 * // Osserva sessione
 * sessionDataStore.userFlow.collect { user ->
 *     if (user != null) { /* utente loggato */ }
 * }
 *
 * // Logout
 * sessionDataStore.clear()
 * ```
 *
 * @property context Contesto Android per accedere al DataStore
 * @property dataStore DataStore personalizzato (opzionale, per test)
 */
class SessionDataStore(
    private val context: Context,
    private val dataStore: DataStore<Preferences>? = null
) {
    private val KEY_USER_JSON = stringPreferencesKey("user_json")
    private val gson = Gson()

    // Usa lazy val per garantire una singola istanza di DataStore
    private val actualDataStore: DataStore<Preferences> by lazy {
        dataStore ?: context.dataStore
    }

    /**
     * Flow reattivo che emette l'utente correntemente loggato.
     * Emette null se nessun utente è autenticato.
     *
     * Il Flow si aggiorna automaticamente quando la sessione cambia
     * (login, logout, o modifica profilo).
     */
    val userFlow: Flow<User?> = actualDataStore.data.map { prefs ->
        prefs[KEY_USER_JSON]?.let { json -> gson.fromJson(json, User::class.java) }
    }

    /**
     * Salva la sessione utente nel DataStore.
     *
     * L'utente viene serializzato in JSON e persistito.
     * L'operazione è atomica e thread-safe.
     *
     * @param user Utente autenticato da salvare
     */
    suspend fun saveUser(user: User) {
        actualDataStore.edit { prefs ->
            prefs[KEY_USER_JSON] = gson.toJson(user)
        }
    }

    /**
     * Cancella la sessione utente corrente (logout).
     *
     * Rimuove tutti i dati della sessione dal DataStore.
     * Dopo questa chiamata, userFlow emetterà null.
     */
    suspend fun clear() {
        actualDataStore.edit { prefs ->
            prefs.remove(KEY_USER_JSON)
        }
    }
}


