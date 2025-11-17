package it.unito.smartshopmobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "categorie_catalogo")
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nome")
    val nome: String,

    @ColumnInfo(name = "descrizione")
    val descrizione: String? = null,

    @ColumnInfo(name = "prodotti_totali")
    val prodottiTotali: Int = 0
)
