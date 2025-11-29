/**
 * Supplier.kt
 *
 * MVVM: Model Layer - Entity fornitore (Room + API)
 *
 * FUNZIONAMENTO:
 * - Rappresenta fornitore per riordini magazzino
 * - Contatti: telefono, email, indirizzo
 * - Cache locale per UI manager
 * - Usato in form riordino merce
 *
 * PATTERN MVVM:
 * - Entity: mapping database/API
 * - @Entity: Room cache table
 * - @SerializedName: mapping JSON
 * - Nullable fields: contatti opzionali
 */
package it.unito.smartshopmobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "fornitori_cache")
data class Supplier(
    @PrimaryKey
    @SerializedName("id_fornitore")
    @ColumnInfo(name = "id_fornitore")
    val id: Int,
    @SerializedName("nome")
    @ColumnInfo(name = "nome")
    val name: String,
    @SerializedName("telefono")
    @ColumnInfo(name = "telefono")
    val phone: String?,
    @SerializedName("email")
    @ColumnInfo(name = "email")
    val email: String?,
    @SerializedName("indirizzo")
    @ColumnInfo(name = "indirizzo")
    val address: String?
)
