package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categorie_catalogo ORDER BY nome ASC")
    fun getAllCategories(): Flow<List<Category>>

    // Sovracategorie distinte derivate dal campo parent
    @Query("""
        SELECT 
            DISTINCT parent_id AS id,
            COALESCE(parent_name, 'Altro') AS nome,
            NULL AS descrizione,
            NULL AS parent_id,
            NULL AS parent_name,
            0 AS prodotti_totali
        FROM categorie_catalogo
        WHERE parent_id IS NOT NULL
        ORDER BY nome
    """)
    fun getMacroCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categorie_catalogo WHERE parent_id = :parentId ORDER BY nome ASC")
    fun getSubcategories(parentId: String): Flow<List<Category>>

    @Query("SELECT * FROM categorie_catalogo WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Query("DELETE FROM categorie_catalogo")
    suspend fun deleteAll()
}

