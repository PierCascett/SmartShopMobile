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
