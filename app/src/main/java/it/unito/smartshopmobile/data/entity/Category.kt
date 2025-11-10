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

    @ColumnInfo(name = "titolo")
    val titolo: String,

    @ColumnInfo(name = "gruppo")
    val gruppo: String,

    @ColumnInfo(name = "ordine")
    val ordine: Int = 0,

    @SerializedName(value = "parentId", alternate = ["parent_id"])
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null
)

