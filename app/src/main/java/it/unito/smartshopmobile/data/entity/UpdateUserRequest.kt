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
