/**
 * Category.kt
 *
 * MVVM: Model Layer - Entity categoria prodotti (Room + API)
 *
 * FUNZIONAMENTO:
 * - Rappresenta categoria con gerarchia (parent/subcategories)
 * - parentId/parentName: riferimento a sovracategoria
 * - prodottiTotali: contatore per UI
 * - Usata per filtri e navigazione catalogo
 *
 * PATTERN MVVM:
 * - Entity: mapping database/API
 * - @Entity: Room table definition
 * - Hierarchy: self-referencing con parentId
 * - Immutabile: data class con val
 */
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

    // Sovracategoria
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,

    @ColumnInfo(name = "parent_name")
    val parentName: String? = null,

    @ColumnInfo(name = "prodotti_totali")
    val prodottiTotali: Int = 0
)
