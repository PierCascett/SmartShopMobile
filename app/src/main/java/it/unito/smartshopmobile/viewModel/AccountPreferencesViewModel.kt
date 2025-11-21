package it.unito.smartshopmobile.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.datastore.AccountPreferencesDataStore
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = AccountPreferencesDataStore(application)

    val preferences: StateFlow<AccountPreferences> = dataStore.data
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountPreferences())

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                RetrofitInstance.overrideBackend(prefs.backendHost, prefs.backendPort)
            }
        }
    }

    fun updateProfile(nome: String, cognome: String, indirizzo: String) {
        viewModelScope.launch {
            dataStore.updateProfile(nome, cognome, indirizzo)
        }
    }

    fun updateBackend(host: String, port: String) {
        viewModelScope.launch {
            dataStore.updateBackend(host, port)
            RetrofitInstance.overrideBackend(host, port)
        }
    }
}

