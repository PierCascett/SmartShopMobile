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

/**
 * Data Access Object per l'accesso ai fornitori nel database Room.
 *
 * Gestisce la cache locale dei fornitori utilizzati dal manager
 * per creare riordini di merce quando le scorte sono basse.
 *
 * Caratteristiche principali:
 * - Ordinamento alfabetico per nome fornitore
 * - Flow reattivo per UI form riordini
 * - Cache pattern per dati relativamente statici
 */
@Dao
interface SupplierDao {
    /**
     * Osserva tutti i fornitori ordinati alfabeticamente per nome.
     *
     * @return Flow che emette lista di tutti i fornitori ordinati per nome
     */
    @Query("SELECT * FROM fornitori_cache ORDER BY nome")
    fun getSuppliers(): Flow<List<Supplier>>

    /**
     * Inserisce o aggiorna una lista di fornitori.
     *
     * @param items Lista di fornitori da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Supplier>)

    /**
     * Elimina tutti i fornitori dalla cache.
     */
    @Query("DELETE FROM fornitori_cache")
    suspend fun clearAll()
}
