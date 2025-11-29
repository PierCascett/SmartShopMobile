/**
 * AccountPreferencesDataStore.kt
 *
 * MVVM: Model Layer - Persistenza preferenze account (DataStore)
 *
 * FUNZIONAMENTO:
 * - Salva/carica dati profilo locale (nome, cognome, indirizzo, telefono)
 * - Cache locale per form pre-compilati
 * - Flow<AccountPreferences> per osservazione reattiva
 * - Update rapido senza chiamate API
 *
 * PATTERN MVVM:
 * - Data persistence: preferenze account
 * - Flow: stream reattivo con data class
 * - suspend fun: operazioni asincrone
 * - DataStore: persistenza key-value moderna
 */
package it.unito.smartshopmobile.data.datastore


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accountPrefsDataStore by preferencesDataStore(name = "account_preferences")

data class AccountPreferences(
    val nome: String = "",
    val cognome: String = "",
    val indirizzoSpedizione: String = "",
    val telefono: String = ""
)

class AccountPreferencesDataStore(private val context: Context) {
    private val KEY_NOME = stringPreferencesKey("nome")
    private val KEY_COGNOME = stringPreferencesKey("cognome")
    private val KEY_INDIRIZZO = stringPreferencesKey("indirizzo_spedizione")
    private val KEY_TELEFONO = stringPreferencesKey("telefono")

    val data: Flow<AccountPreferences> = context.accountPrefsDataStore.data.map { prefs ->
        AccountPreferences(
            nome = prefs[KEY_NOME].orEmpty(),
            cognome = prefs[KEY_COGNOME].orEmpty(),
            indirizzoSpedizione = prefs[KEY_INDIRIZZO].orEmpty(),
            telefono = prefs[KEY_TELEFONO].orEmpty()
        )
    }

    suspend fun updateProfile(nome: String, cognome: String, indirizzo: String, telefono: String) {
        context.accountPrefsDataStore.edit { prefs ->
            prefs[KEY_NOME] = nome
            prefs[KEY_COGNOME] = cognome
            prefs[KEY_INDIRIZZO] = indirizzo
            prefs[KEY_TELEFONO] = telefono
        }
    }
}

