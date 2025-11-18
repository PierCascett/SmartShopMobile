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
    indices = [
        Index(value = ["categoria_id"]),
        Index(value = ["id"]) // indice su productId per ricerche veloci
    ]
)
data class Product(
    // Chiave primaria: riga di catalogo (univoca per posizione/scaffale)
    @PrimaryKey
    @ColumnInfo(name = "catalog_id")
    val catalogId: Int,

    // ID prodotto logico (può ripetersi su più righe catalogo)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nome")
    val name: String,

    @ColumnInfo(name = "marca")
    val brand: String,

    @ColumnInfo(name = "categoria_id")
    val categoryId: String,

    @ColumnInfo(name = "categoria_nome")
    val categoryName: String? = null,

    @ColumnInfo(name = "categoria_descrizione")
    val categoryDescription: String? = null,

    @ColumnInfo(name = "catalog_quantity")
    val catalogQuantity: Int = 0,

    @ColumnInfo(name = "warehouse_quantity")
    val warehouseQuantity: Int = 0,

    @ColumnInfo(name = "total_quantity")
    val totalQuantity: Int = 0,

    @ColumnInfo(name = "prezzo")
    val price: Double,

    @ColumnInfo(name = "vecchio_prezzo")
    val oldPrice: Double? = null,

    @ColumnInfo(name = "disponibilita")
    val availability: String,

    @ColumnInfo(name = "tag")
    val tags: List<String>? = null,

    @ColumnInfo(name = "descrizione")
    val description: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    // id scaffale da catalogo (se disponibile nel payload)
    @ColumnInfo(name = "id_scaffale")
    val shelfId: Int? = null
)
