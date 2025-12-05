/**
 * AccountPreferencesViewModel.kt
 *
 * MVVM: ViewModel Layer - Gestione preferenze account
 *
 * FUNZIONAMENTO:
 * - Gestisce dati profilo locale (cache veloce)
 * - Espone StateFlow<AccountPreferences> per UI
 * - Update preferenze senza chiamate API
 * - Usato per pre-compilare form checkout
 *
 * PATTERN MVVM:
 * - ViewModel: logica presentazione preferenze
 * - StateFlow: stato reattivo
 * - DataStore: persistenza locale
 * - Intent: updateProfile()
 */
package it.unito.smartshopmobile.viewModel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.datastore.AccountPreferencesDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel che espone e aggiorna le preferenze account memorizzate in DataStore.
 *
 * Espone un `StateFlow<AccountPreferences>` per la UI e fornisce un'unica entry point
 * `updateProfile` per salvare i campi del profilo locale (nome, cognome, indirizzo, telefono).
 * Non effettua chiamate di rete: funge da cache rapida per form e dati utente.
 */
class AccountPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = AccountPreferencesDataStore(application)

    val preferences: StateFlow<AccountPreferences> = dataStore.data
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountPreferences())

    /**
     * Aggiorna in DataStore tutti i campi del profilo locale.
     *
     * @param nome Nome utente
     * @param cognome Cognome utente
     * @param indirizzo Indirizzo di spedizione
     * @param telefono Numero di telefono
     */
    fun updateProfile(nome: String, cognome: String, indirizzo: String, telefono: String) {
        viewModelScope.launch {
            dataStore.updateProfile(nome, cognome, indirizzo, telefono)
        }
    }
}
