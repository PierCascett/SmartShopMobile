/**
 * ShelfDao.kt
 * 
 * MVVM: Model Layer - DAO per scaffali (Room)
 * 
 * FUNZIONAMENTO:
 * - Query scaffali ordinati per ID
 * - Espone Flow per mappa interattiva (picking dipendente)
 * - Sincronizzazione con API: insert/delete batch
 * 
 * PATTERN MVVM:
 * - DAO Pattern: accesso database scaffali
 * - Flow<List<Shelf>>: stream reattivo per UI mappa
 * - suspend fun: insert/delete asincrone
 * - OnConflictStrategy.REPLACE: sync da API
 */
package it.unito.smartshopmobile.data.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Shelf
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per l'accesso agli scaffali nel database Room.
 *
 * Gestisce la cache locale degli scaffali fisici del supermercato,
 * utilizzati per la mappa interattiva e il picking degli ordini.
 *
 * Caratteristiche principali:
 * - Ordinamento per ID scaffale crescente
 * - Flow reattivo per aggiornamenti mappa in tempo reale
 * - Sincronizzazione batch con API
 */
@Dao
interface ShelfDao {
    /**
     * Osserva tutti gli scaffali ordinati per ID.
     *
     * @return Flow che emette lista di tutti gli scaffali ordinati per ID
     */
    @Query("SELECT * FROM scaffali ORDER BY id_scaffale ASC")
    fun getAll(): Flow<List<Shelf>>

    /**
     * Inserisce o aggiorna una lista di scaffali.
     *
     * @param items Lista di scaffali da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Shelf>)

    /**
     * Elimina tutti gli scaffali dalla tabella.
     */
    @Query("DELETE FROM scaffali")
    suspend fun deleteAll()
}
