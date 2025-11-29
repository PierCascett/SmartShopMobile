/**
 * SupplierDao.kt
 *
 * MVVM: Model Layer - DAO per fornitori (Room)
 *
 * FUNZIONAMENTO:
 * - Query fornitori ordinati alfabeticamente
 * - Cache locale elenco fornitori per riordini
 * - Sync da API: insert/clear batch
 *
 * PATTERN MVVM:
 * - DAO Pattern: accesso database fornitori
 * - Flow<List<Supplier>>: stream reattivo
 * - suspend fun: operazioni asincrone
 * - Cache pattern: fornitori_cache table
 */
package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM fornitori_cache ORDER BY nome")
    fun getSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Supplier>)

    @Query("DELETE FROM fornitori_cache")
    suspend fun clearAll()
}
