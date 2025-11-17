package it.unito.smartshopmobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Scaffale del supermercato (usato per mappa e catalogo).
 * In DB: tabella scaffali, già popolata da dump.
 */
@Entity(tableName = "scaffali")
data class Shelf(
    @PrimaryKey
    @SerializedName("id_scaffale")
    @ColumnInfo(name = "id_scaffale")
    val id: Int,
    @SerializedName("nome")
    @ColumnInfo(name = "nome")
    val nome: String,
    @SerializedName("descrizione")
    @ColumnInfo(name = "descrizione")
    val descrizione: String? = null
)

