/**
 * RestockDao.kt
 *
 * MVVM: Model Layer - DAO per riordini magazzino (Room)
 *
 * FUNZIONAMENTO:
 * - Query riordini ordinati per data (DESC)
 * - Cache locale per storico riordini manager
 * - Sincronizzazione batch con API fornitori
 *
 * PATTERN MVVM:
 * - DAO Pattern: accesso database riordini
 * - Flow<List<Restock>>: stream reattivo per UI manager
 * - suspend fun: insert/delete asincrone
 * - Cache pattern: riordini_cache table
 */
package it.unito.smartshopmobile.data.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Restock
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per l'accesso ai riordini magazzino nel database Room.
 *
 * Gestisce la cache locale dei riordini creati dal manager,
 * fornendo uno storico completo delle operazioni di rifornimento.
 *
 * Caratteristiche principali:
 * - Ordinamento per data ordine decrescente (più recenti prima)
 * - Flow reattivo per UI manager
 * - Tracking completo del ciclo di vita dei riordini
 */
@Dao
interface RestockDao {
    /**
     * Osserva tutti i riordini ordinati per data decrescente.
     *
     * I riordini più recenti appaiono per primi nella lista.
     *
     * @return Flow che emette lista di tutti i riordini ordinati per data
     */
    @Query("SELECT * FROM riordini_cache ORDER BY data_ordine DESC")
    fun getRestocks(): Flow<List<Restock>>

    /**
     * Inserisce o aggiorna una lista di riordini.
     *
     * @param items Lista di riordini da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Restock>)

    /**
     * Elimina tutti i riordini dalla cache.
     */
    @Query("DELETE FROM riordini_cache")
    suspend fun deleteAll()
}
