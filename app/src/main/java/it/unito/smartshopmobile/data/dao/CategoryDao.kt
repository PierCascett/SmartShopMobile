package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categorie_catalogo ORDER BY ordine ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categorie_catalogo WHERE gruppo = 'macro' ORDER BY ordine ASC")
    fun getMacroCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categorie_catalogo WHERE parent_id = :parentId ORDER BY ordine ASC")
    fun getSubcategories(parentId: String): Flow<List<Category>>

    @Query("SELECT * FROM categorie_catalogo WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Query("DELETE FROM categorie_catalogo")
    suspend fun deleteAll()
}

