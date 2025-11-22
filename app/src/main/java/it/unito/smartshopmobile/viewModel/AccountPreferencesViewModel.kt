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

class AccountPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = AccountPreferencesDataStore(application)

    val preferences: StateFlow<AccountPreferences> = dataStore.data
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountPreferences())

    fun updateProfile(nome: String, cognome: String, indirizzo: String, telefono: String) {
        viewModelScope.launch {
            dataStore.updateProfile(nome, cognome, indirizzo, telefono)
        }
    }
}

