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

/**
 * Data Access Object per l'accesso agli ordini nel database Room.
 *
 * Gestisce ordini e righe ordine con relazione 1-N utilizzando
 * @Transaction per garantire l'atomicità delle query con join.
 * Fornisce cache locale per funzionamento offline-first.
 *
 * Caratteristiche principali:
 * - Query transazionali per ordini completi (Order + OrderLine)
 * - Flow reattivi per osservazione automatica
 * - Ordinamento per data decrescente (ordini più recenti prima)
 * - Operazioni batch per sincronizzazione API
 */
@Dao
interface OrderDao {
    /**
     * Osserva tutti gli ordini con le relative righe ordine.
     *
     * Utilizza @Transaction per garantire che Order e OrderLine siano
     * caricati atomicamente. Gli ordini sono ordinati per data decrescente.
     *
     * @return Flow che emette lista di OrderWithLines (ordini completi)
     */
    @Transaction
    @Query("SELECT * FROM ordini_cache ORDER BY data_ordine DESC")
    fun getOrdersWithLines(): Flow<List<OrderWithLines>>

    /**
     * Inserisce o aggiorna una lista di ordini.
     *
     * @param orders Lista di ordini da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    /**
     * Inserisce o aggiorna una lista di righe ordine.
     *
     * @param lines Lista di righe ordine da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<OrderLine>)

    /**
     * Elimina tutti gli ordini dalla cache.
     */
    @Query("DELETE FROM ordini_cache")
    suspend fun deleteAllOrders()

    /**
     * Elimina tutte le righe ordine dalla cache.
     */
    @Query("DELETE FROM righe_ordine_cache")
    suspend fun deleteAllLines()
}
