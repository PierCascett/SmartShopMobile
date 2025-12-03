/**
 * CategoryDao.kt
 *
 * MVVM: Model Layer - DAO per categorie (Room)
 *
 * FUNZIONAMENTO:
 * - Query per categorie e macro-categorie (gerarchia parent)
 * - Espone Flow per osservazione reattiva
 * - CRUD: insert, query con ordinamento, delete
 * - Query SQL complesse per gerarchie
 *
 * PATTERN MVVM:
 * - DAO Pattern: astrazione accesso database
 * - Flow<List<Category>>: stream reattivo categorie
 * - suspend fun: operazioni write asincrone
 * - Room annotations: @Query, @Insert con conflict strategy
 */
package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("""
        SELECT * FROM categorie_catalogo ORDER BY nome ASC
    """)
    fun getAllCategories(): Flow<List<Category>>

    @Query("""
        SELECT
            DISTINCT parent_id AS id,
            parent_id AS nome,
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

