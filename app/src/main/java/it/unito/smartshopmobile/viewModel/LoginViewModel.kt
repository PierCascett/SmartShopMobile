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
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel che gestisce autenticazione e stato di login.
 *
 * Espone i campi input e flag di stato come `MutableState` per Compose, valida i dati,
 * invoca l'`AuthRepository` per login/registrazione e salva l'utente in `SessionDataStore`,
 * notificando la UI tramite `loginSuccessUser`.
 */
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

    /** Imposta l'email inserita dall'utente. */
    fun setEmail(value: String) {
        email.value = value
    }

    /** Imposta la password inserita dall'utente. */
    fun setPassword(value: String) {
        password.value = value
    }

    /** Pulisce l'ultimo errore mostrato in UI. */
    fun clearError() {
        errorMessage.value = null
    }

    /** Resetta il flag di successo login per evitare trigger multipli. */
    fun clearLoginSuccess() {
        loginSuccessUser.value = null
    }

    /**
     * Esegue il login tramite AuthRepository dopo una validazione minima.
     * Aggiorna loading/error e, su successo, salva la sessione e notifica la UI.
     */
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

    /**
     * Registra un nuovo utente con i campi inseriti e salva la sessione su successo.
     * Esegue validazione basica dei campi obbligatori.
     */
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

    /** Salva l'utente autenticato in DataStore (persistenza sessione). */
    private fun saveSession(user: User) {
        viewModelScope.launch {
            sessionDataStore.saveUser(user)
        }
    }

    /** Cancella la sessione persistita (logout locale). */
    fun clearSession() {
        viewModelScope.launch {
            sessionDataStore.clear()
        }
    }
}
