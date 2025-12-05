/**
 * UpdateUserRequest.kt
 *
 * MVVM: Model Layer - DTO aggiornamento profilo (API)
 *
 * FUNZIONAMENTO:
 * - Request per API PATCH /users/:id
 * - Campi opzionali (nullable) per update parziali
 * - Usato da UserRepository.updateProfile()
 * - Serializzato in JSON per backend
 *
 * PATTERN MVVM:
 * - DTO: comunicazione API
 * - @SerializedName: mapping JSON
 * - Nullable fields: update parziali
 * - Immutabile: data class
 */
package it.unito.smartshopmobile.data.entity

import com.google.gson.annotations.SerializedName

/**
 * DTO per la richiesta di aggiornamento profilo utente all'API.
 *
 * Utilizzato per inviare aggiornamenti parziali del profilo utente.
 * Tutti i campi sono opzionali (nullable) per permettere update selettivi:
 * solo i campi non-null vengono inclusi nella richiesta JSON.
 *
 * Endpoint tipico: PATCH /api/users/:id
 *
 * Esempio:
 * ```kotlin
 * // Aggiorna solo nome e telefono
 * UpdateUserRequest(
 *     nome = "Mario",
 *     cognome = null,  // Non modificato
 *     email = null,    // Non modificato
 *     telefono = "1234567890"
 * )
 * ```
 *
 * @property nome Nuovo nome dell'utente (null se non da modificare)
 * @property cognome Nuovo cognome (null se non da modificare)
 * @property email Nuova email (null se non da modificare)
 * @property telefono Nuovo telefono (null se non da modificare)
 */
data class UpdateUserRequest(
    @SerializedName("nome")
    val nome: String?,
    @SerializedName("cognome")
    val cognome: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("telefono")
    val telefono: String?
)
