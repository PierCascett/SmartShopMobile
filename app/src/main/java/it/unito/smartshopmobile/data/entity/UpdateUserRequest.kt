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
