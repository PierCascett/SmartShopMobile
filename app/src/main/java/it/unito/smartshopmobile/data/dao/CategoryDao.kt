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

    // Nel nuovo schema non ci sono macro/sotto categorie: ritorniamo tutte
    @Query("SELECT * FROM categorie_catalogo ORDER BY nome ASC")
    fun getMacroCategories(): Flow<List<Category>>

    // Compat: al momento non esistono sottocategorie, uso il parametro per evitare warning KSP
    @Query("SELECT * FROM categorie_catalogo WHERE 1 = 0 AND :parentId IS NOT NULL")
    fun getSubcategories(parentId: String): Flow<List<Category>>

    @Query("SELECT * FROM categorie_catalogo WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Query("DELETE FROM categorie_catalogo")
    suspend fun deleteAll()
}

