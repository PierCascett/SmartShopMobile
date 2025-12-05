/**
 * FakePreferencesDataStore.kt
 *
 * Implementazione fake di DataStore<Preferences> per i test.
 *
 * Questa implementazione utilizza un MutableStateFlow in memoria invece di
 * accedere al file system, evitando i problemi di concorrenza e rename
 * che si verificano con Robolectric su Windows.
 */
package it.unito.smartshopmobile.testUtils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * DataStore fake che mantiene i dati in memoria.
 *
 * Utile per i test di integrazione dove non è necessario testare
 * la persistenza su disco, ma solo il comportamento del codice
 * che utilizza DataStore.
 *
 * Uso:
 * ```kotlin
 * val fakeDataStore = FakePreferencesDataStore()
 * val sessionDataStore = SessionDataStore(context, fakeDataStore)
 * ```
 */
class FakePreferencesDataStore : DataStore<Preferences> {

    private val _data = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return _data.updateAndGet { current ->
            transform(current)
        }
    }

    /**
     * Resetta il DataStore allo stato iniziale (vuoto).
     * Utile nel tearDown dei test.
     */
    fun reset() {
        _data.value = emptyPreferences()
    }
}

