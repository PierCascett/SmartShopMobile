/**
 * OrderDao.kt
 *
 * MVVM: Model Layer - DAO per ordini (Room)
 *
 * FUNZIONAMENTO:
 * - Gestisce ordini e righe ordine (relazione 1-N)
 * - @Transaction: garantisce atomicità query con join
 * - Espone Flow<OrderWithLines> per osservare ordini completi
 * - Cache locale per sync offline-first
 *
 * PATTERN MVVM:
 * - DAO Pattern: accesso database type-safe
 * - Flow: reattività automatica
 * - @Transaction: query con relazioni (Order + Lines)
 * - OnConflictStrategy.REPLACE: upsert automatico
 */
package it.unito.smartshopmobile.data.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.OrderLine
import it.unito.smartshopmobile.data.entity.OrderWithLines
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM ordini_cache ORDER BY data_ordine DESC")
    fun getOrdersWithLines(): Flow<List<OrderWithLines>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<OrderLine>)

    @Query("DELETE FROM ordini_cache")
    suspend fun deleteAllOrders()

    @Query("DELETE FROM righe_ordine_cache")
    suspend fun deleteAllLines()
}

