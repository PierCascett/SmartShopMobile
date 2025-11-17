/**
 * LoginViewModel.kt
 *
 * RUOLO MVVM: ViewModel Layer
 * - Gestisce lo stato UI e la logica di autenticazione
 * - Valida credenziali e determina il routing post-login
 * - Intermedia tra UI (LoginScreen) e futuro AuthRepository
 *
 * RESPONSABILITÀ:
 * - Gestire campi input (email, password)
 * - Validare formato email e password
 * - Simulare login (futuro: chiamata API tramite AuthRepository)
 * - Determinare UserRole in base all'email (customer/employee/manager)
 * - Esporre stato loading e messaggi di errore
 *
 * PATTERN: MVVM (Model-View-ViewModel)
 * - Stato osservabile tramite MutableState
 * - Intent utente: login(), setEmail(), setPassword()
 * - Logica di validazione centralizzata
 *
 * FUTURE IMPROVEMENTS:
 * - Usare AuthRepository invece di logica hardcoded
 * - Usare sealed class LoginUiState per stato più robusto
 * - Dependency Injection (Hilt) per il repository
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.AuthRepository
import kotlinx.coroutines.launch
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

// ViewModel che gestisce lo stato di login dell'app
// - espone campi osservabili (email, password, loading, error, successo)
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(RetrofitInstance.api)
    private val sessionDataStore = SessionDataStore(application)

    // Stato semplice usato dalla UI
    val email: MutableState<String> = mutableStateOf("")
    val password: MutableState<String> = mutableStateOf("")
    val nome: MutableState<String> = mutableStateOf("")
    val cognome: MutableState<String> = mutableStateOf("")
    val telefono: MutableState<String> = mutableStateOf("")
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val errorMessage: MutableState<String?> = mutableStateOf(null)
    val loginSuccessUser: MutableState<User?> = mutableStateOf(null)
    val sessionUser: StateFlow<User?> =
        sessionDataStore.userFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setEmail(value: String) {
        email.value = value
    }

    fun setPassword(value: String) {
        password.value = value
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun clearLoginSuccess() {
        loginSuccessUser.value = null
    }

    // Metodo di login (simulato): valida campi, simula chiamata di rete e imposta stato di successo
    fun login() {
        if (email.value.isBlank() || password.value.isBlank()) {
            errorMessage.value = "Inserisci email e password"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            val result = authRepository.login(email.value.trim(), password.value)
            isLoading.value = false
            result.onSuccess { user ->
                saveSession(user)
                loginSuccessUser.value = user
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Errore di login"
            }
        }
    }

    fun register() {
        if (email.value.isBlank() || password.value.isBlank() || nome.value.isBlank() || cognome.value.isBlank()) {
            errorMessage.value = "Nome, cognome, email e password sono obbligatori"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            val result = authRepository.register(
                nome = nome.value.trim(),
                cognome = cognome.value.trim(),
                email = email.value.trim(),
                telefono = telefono.value.trim().ifBlank { null },
                password = password.value
            )
            isLoading.value = false
            result.onSuccess { user ->
                saveSession(user)
                loginSuccessUser.value = user
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Errore di registrazione"
            }
        }
    }

    private fun saveSession(user: User) {
        viewModelScope.launch {
            sessionDataStore.saveUser(user)
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            sessionDataStore.clear()
        }
    }
}
