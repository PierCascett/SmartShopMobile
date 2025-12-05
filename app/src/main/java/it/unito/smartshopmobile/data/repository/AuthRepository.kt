/**
 * AuthRepository.kt
 *
 * MVVM: Model Layer - Repository per autenticazione
 *
 * FUNZIONAMENTO:
 * - Gestisce login e registrazione tramite API REST
 * - Coordina chiamate di rete (Retrofit)
 * - Restituisce Result<User> per gestione errori type-safe
 * - Astrae la sorgente dati dal ViewModel
 *
 * PATTERN MVVM:
 * - Repository Pattern: astrazione tra ViewModel e sorgente dati
 * - suspend fun: integrazione con Coroutines
 * - Result<T>: gestione errori funzionale
 * - Single Responsibility: solo logica autenticazione
 */
package it.unito.smartshopmobile.data.repository

import android.util.Log
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.SmartShopApiService

/**
 * Repository per la gestione dell'autenticazione utente.
 *
 * Coordina le operazioni di login e registrazione con il backend API,
 * gestendo la comunicazione di rete e la trasformazione degli errori
 * in Result type-safe per i ViewModel.
 *
 * Caratteristiche principali:
 * - Pattern Repository: astrazione tra ViewModel e sorgente dati
 * - Result<T>: gestione errori funzionale e type-safe
 * - Suspend functions: integrazione con Kotlin Coroutines
 * - Logging degli errori per debugging
 *
 * Utilizzo tipico:
 * ```kotlin
 * authRepository.login(email, password).fold(
 *     onSuccess = { user -> /* login riuscito */ },
 *     onFailure = { error -> /* gestisci errore */ }
 * )
 * ```
 *
 * @property apiService Servizio API Retrofit per chiamate di rete
 */
class AuthRepository(
    private val apiService: SmartShopApiService
) {
    /**
     * Effettua il login con email e password.
     *
     * Invia una richiesta POST al backend con le credenziali fornite.
     * In caso di successo, restituisce l'oggetto User completo.
     *
     * @param email Email dell'utente
     * @param password Password dell'utente
     * @return Result<User> contenente l'utente autenticato o un errore
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(mapOf("email" to email, "password" to password))
            if (response.isSuccessful) {
                response.body()?.get("user")?.let { Result.success(it) }
                    ?: Result.failure(Exception("Risposta vuota dal server"))
            } else {
                Result.failure(Exception("Login fallito (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Errore login", e)
            Result.failure(e)
        }
    }

    /**
     * Registra un nuovo utente nel sistema.
     *
     * Invia una richiesta POST al backend con i dati di registrazione.
     * In caso di successo, l'utente viene automaticamente autenticato
     * e i suoi dati vengono restituiti.
     *
     * @param nome Nome dell'utente
     * @param cognome Cognome dell'utente
     * @param email Email univoca per il login
     * @param telefono Numero di telefono (opzionale)
     * @param password Password per l'account
     * @return Result<User> contenente l'utente registrato o un errore
     */
    suspend fun register(
        nome: String,
        cognome: String,
        email: String,
        telefono: String?,
        password: String
    ): Result<User> {
        return try {
            val response = apiService.register(
                mapOf(
                    "nome" to nome,
                    "cognome" to cognome,
                    "email" to email,
                    "telefono" to telefono,
                    "password" to password
                )
            )
            if (response.isSuccessful) {
                response.body()?.get("user")?.let { Result.success(it) }
                    ?: Result.failure(Exception("Risposta vuota dal server"))
            } else {
                Result.failure(Exception("Registrazione fallita (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Errore registrazione", e)
            Result.failure(e)
        }
    }
}
