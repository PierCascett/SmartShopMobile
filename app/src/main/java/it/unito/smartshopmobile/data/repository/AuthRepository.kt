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
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import it.unito.smartshopmobile.data.entity.User

class AuthRepository(
    private val apiService: SmartShopApiService
) {
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
