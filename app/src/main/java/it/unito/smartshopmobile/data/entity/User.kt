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

