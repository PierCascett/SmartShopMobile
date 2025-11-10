package it.unito.smartshopmobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prodotti_catalogo",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoria_id"])]
)
data class Product(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nome")
    val name: String,

    @ColumnInfo(name = "marca")
    val brand: String,

    @ColumnInfo(name = "categoria_id")
    val categoryId: String,

    @ColumnInfo(name = "prezzo")
    val price: Float,

    @ColumnInfo(name = "vecchio_prezzo")
    val oldPrice: Float? = null,

    @ColumnInfo(name = "disponibilita")
    val availability: String,

    @ColumnInfo(name = "tag")
    val tags: String? = null,

    @ColumnInfo(name = "descrizione")
    val description: String? = null
)

