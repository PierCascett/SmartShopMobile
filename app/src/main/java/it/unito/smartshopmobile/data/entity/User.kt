/**
 * User.kt
 *
 * MVVM: Model Layer - Entity utente
 *
 * FUNZIONAMENTO:
 * - Rappresenta utente autenticato (Customer/Employee/Manager)
 * - Usato sia per API response (Retrofit) che per persistenza locale (DataStore)
 * - @SerializedName: mapping campi JSON API → proprietà Kotlin
 * - data class: immutabilità, equals/hashCode automatici
 *
 * PATTERN MVVM:
 * - Entity/DTO: Data Transfer Object tra layer
 * - Serializzazione JSON con Gson
 * - Condiviso tra Repository, ViewModel e DataStore
 *
 * CAMPI:
 * - id: ID univoco utente (database backend)
 * - nome/cognome: informazioni anagrafiche
 * - email: usata per login, univoca
 * - telefono: opzionale, contatto
 * - avatarUrl: opzionale, immagine profilo
 * - ruolo: "customer" | "employee" | "manager" → determina UserRole
 */
package it.unito.smartshopmobile.data.entity

import com.google.gson.annotations.SerializedName

/**
 * Entity condivisa (API + uso in memoria) per l'utente autenticato.
 */
data class User(
    @SerializedName("id_utente")
    val id: Int,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("cognome")
    val cognome: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("telefono")
    val telefono: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("ruolo")
    val ruolo: String
)

