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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ViewModel che gestisce lo stato di login dell'app
// - espone campi osservabili (email, password, loading, error, successo)
class LoginViewModel : ViewModel() {
    // Stato semplice usato dalla UI
    val email: MutableState<String> = mutableStateOf("")
    val password: MutableState<String> = mutableStateOf("")
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val errorMessage: MutableState<String?> = mutableStateOf(null)
    val loginSuccessEmail: MutableState<String?> = mutableStateOf(null)

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
        loginSuccessEmail.value = null
    }

    // Metodo di login (simulato): valida campi, simula chiamata di rete e imposta stato di successo
    fun login() {
        if (email.value.isBlank() || password.value.isBlank()) {
            errorMessage.value = "Inserisci email e password"
            return
        }

        // Esempio finto di autenticazione
        viewModelScope.launch {
            isLoading.value = true
            // simulazione rete
            delay(700)
            isLoading.value = false

            // per ora consideriamo il login sempre riuscito
            loginSuccessEmail.value = email.value.trim()
        }
    }
}