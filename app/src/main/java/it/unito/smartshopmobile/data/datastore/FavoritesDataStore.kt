/**
 * FavoritesDataStore.kt
 *
 * MVVM: Model Layer - Persistenza preferiti cliente (DataStore)
 *
 * FUNZIONAMENTO:
 * - Salva/carica prodotti preferiti per utente
 * - Set<String> di productId per cliente
 * - Separato per userId (evita mix tra account)
 * - Serializzazione JSON con Gson
 *
 * PATTERN MVVM:
 * - Data persistence: preferenze per-utente
 * - Flow<Set<String>>: stream reattivo favoriti
 * - suspend fun: operazioni asincrone
 * - DataStore Preferences: alternativa moderna a SharedPreferences
 */
package it.unito.smartshopmobile.data.datastore


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites_datastore")

/**
 * Gestisce la persistenza dei prodotti preferiti del cliente tramite DataStore.
 *
 * Questa classe fornisce un'astrazione per salvare e ripristinare l'insieme
 * di ID prodotti preferiti per ogni utente. I dati sono separati per userId
 * per evitare la commistione tra account diversi.
 *
 * Caratteristiche principali:
 * - Persistenza per-utente (key: favorites_$userId)
 * - Set<String> di productId serializzato in JSON
 * - Flow reattivo per osservare cambiamenti
 * - Operazioni asincrone thread-safe
 *
 * Uso tipico:
 * ```kotlin
 * // Osserva preferiti
 * favoritesDataStore.favoritesFlow(userId).collect { favoriteIds ->
 *     // Aggiorna UI con Set<String> di product IDs
 * }
 *
 * // Salva preferiti
 * favoritesDataStore.saveFavorites(userId, setOf("prod-1", "prod-2"))
 * ```
 *
 * @property context Contesto Android per accedere al DataStore
 * @property dataStore DataStore personalizzato (opzionale, per test)
 */
class FavoritesDataStore(
    private val context: Context,
    private val dataStore: DataStore<Preferences>? = null
) {
    private val gson = Gson()

    // Usa lazy val per garantire una singola istanza di DataStore
    private val actualDataStore: DataStore<Preferences> by lazy {
        dataStore ?: context.favoritesDataStore
    }

    /**
     * Genera la chiave DataStore specifica per un utente.
     *
     * @param userId ID dell'utente
     * @return Chiave DataStore univoca per l'utente
     */
    private fun keyForUser(userId: Int) = stringPreferencesKey("favorites_$userId")

    /**
     * Flow reattivo che emette l'insieme di ID prodotti preferiti per un utente.
     *
     * Il Flow si aggiorna automaticamente quando l'utente aggiunge o rimuove
     * prodotti dai preferiti. Emette un Set vuoto se l'utente non ha preferiti.
     *
     * @param userId ID dell'utente di cui osservare i preferiti
     * @return Flow che emette Set<String> di product IDs preferiti
     */
    fun favoritesFlow(userId: Int): Flow<Set<String>> =
        actualDataStore.data.map { prefs ->
            prefs[keyForUser(userId)]?.let { raw ->
                gson.fromJson<Set<String>>(raw, object : TypeToken<Set<String>>() {}.type)
            } ?: emptySet()
        }

    /**
     * Salva l'insieme completo di prodotti preferiti per un utente.
     *
     * Sovrascrive completamente i preferiti precedenti con il nuovo Set.
     * L'operazione è atomica e thread-safe.
     *
     * @param userId ID dell'utente
     * @param favorites Set di product IDs da salvare come preferiti
     */
    suspend fun saveFavorites(userId: Int, favorites: Set<String>) {
        actualDataStore.edit { prefs ->
            prefs[keyForUser(userId)] = gson.toJson(favorites)
        }
    }
}
