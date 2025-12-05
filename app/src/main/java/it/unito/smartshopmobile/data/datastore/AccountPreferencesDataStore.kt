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

/**
 * Data class immutabile che rappresenta le preferenze dell'account utente.
 *
 * Contiene informazioni di profilo locali che possono essere modificate
 * senza necessariamente sincronizzarle con il backend.
 *
 * @property nome Nome dell'utente
 * @property cognome Cognome dell'utente
 * @property indirizzoSpedizione Indirizzo di spedizione preferito
 * @property telefono Numero di telefono di contatto
 */
data class AccountPreferences(
    val nome: String = "",
    val cognome: String = "",
    val indirizzoSpedizione: String = "",
    val telefono: String = ""
)

/**
 * Gestisce la persistenza delle preferenze account tramite DataStore.
 *
 * Questa classe fornisce un modo per salvare e ripristinare le informazioni
 * di profilo dell'utente localmente, permettendo form pre-compilati e
 * aggiornamenti rapidi senza chiamate di rete immediate.
 *
 * Caratteristiche principali:
 * - Cache locale dei dati profilo
 * - Flow reattivo per osservare modifiche
 * - Update atomico di tutti i campi
 * - Persistenza tra riavvii app
 *
 * Uso tipico:
 * ```kotlin
 * // Osserva preferenze
 * accountPrefsDataStore.data.collect { prefs ->
 *     // Usa prefs.nome, prefs.cognome, etc.
 * }
 *
 * // Aggiorna profilo
 * accountPrefsDataStore.updateProfile(
 *     nome = "Mario",
 *     cognome = "Rossi",
 *     indirizzo = "Via Roma 1",
 *     telefono = "1234567890"
 * )
 * ```
 *
 * @property context Contesto Android per accedere al DataStore
 */
class AccountPreferencesDataStore(private val context: Context) {
    private val KEY_NOME = stringPreferencesKey("nome")
    private val KEY_COGNOME = stringPreferencesKey("cognome")
    private val KEY_INDIRIZZO = stringPreferencesKey("indirizzo_spedizione")
    private val KEY_TELEFONO = stringPreferencesKey("telefono")

    /**
     * Flow reattivo che emette le preferenze account correnti.
     *
     * Il Flow si aggiorna automaticamente quando le preferenze vengono modificate.
     * Emette valori di default (stringhe vuote) se non sono mai state impostate.
     */
    val data: Flow<AccountPreferences> = context.accountPrefsDataStore.data.map { prefs ->
        AccountPreferences(
            nome = prefs[KEY_NOME].orEmpty(),
            cognome = prefs[KEY_COGNOME].orEmpty(),
            indirizzoSpedizione = prefs[KEY_INDIRIZZO].orEmpty(),
            telefono = prefs[KEY_TELEFONO].orEmpty()
        )
    }

    /**
     * Aggiorna tutti i campi del profilo account in un'unica operazione atomica.
     *
     * Tutti i parametri vengono salvati contemporaneamente per garantire
     * la consistenza dei dati.
     *
     * @param nome Nuovo nome dell'utente
     * @param cognome Nuovo cognome dell'utente
     * @param indirizzo Nuovo indirizzo di spedizione
     * @param telefono Nuovo numero di telefono
     */
    suspend fun updateProfile(nome: String, cognome: String, indirizzo: String, telefono: String) {
        context.accountPrefsDataStore.edit { prefs ->
            prefs[KEY_NOME] = nome
            prefs[KEY_COGNOME] = cognome
            prefs[KEY_INDIRIZZO] = indirizzo
            prefs[KEY_TELEFONO] = telefono
        }
    }
}

