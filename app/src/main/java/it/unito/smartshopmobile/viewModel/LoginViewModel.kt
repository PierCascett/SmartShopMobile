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