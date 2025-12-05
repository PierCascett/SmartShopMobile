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

/**
 * Data Access Object per l'accesso alle categorie nel database Room.
 *
 * Gestisce una struttura gerarchica di categorie con supporto per
 * macro-categorie (parent) e sottocategorie (children). Fornisce
 * query ottimizzate per navigazione e filtraggio del catalogo.
 *
 * Caratteristiche principali:
 * - Query SQL complesse per gestione gerarchie
 * - Flow reattivi per osservazione automatica
 * - Ordinamento alfabetico per nome
 * - Supporto per categorie root e figlie
 */
@Dao
interface CategoryDao {
    /**
     * Osserva tutte le categorie ordinate alfabeticamente.
     *
     * @return Flow che emette lista di tutte le categorie ordinate per nome
     */
    @Query("""
        SELECT * FROM categorie_catalogo ORDER BY nome ASC
    """)
    /**
     * Helper per gestire get all categories.
     */
    fun getAllCategories(): Flow<List<Category>>

    /**
     * Osserva le macro-categorie (categorie parent univoche).
     *
     * Utilizza DISTINCT su parent_id per ottenere solo le sovracategorie
     * senza duplicati. Utile per mostrare filtri di primo livello.
     *
     * @return Flow che emette lista di macro-categorie
     */
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
    /**
     * Helper per gestire get macro categories.
     */
    fun getMacroCategories(): Flow<List<Category>>

    /**
     * Osserva le sottocategorie di una specifica categoria parent.
     *
     * @param parentId ID della categoria parent
     * @return Flow che emette lista di sottocategorie ordinate per nome
     */
    @Query("SELECT * FROM categorie_catalogo WHERE parent_id = :parentId ORDER BY nome ASC")
    fun getSubcategories(parentId: String): Flow<List<Category>>

    /**
     * Recupera una categoria specifica per ID.
     *
     * @param id ID della categoria
     * @return Category se trovata, null altrimenti
     */
    @Query("SELECT * FROM categorie_catalogo WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    /**
     * Inserisce o aggiorna una lista di categorie.
     *
     * @param categories Lista di categorie da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    /**
     * Elimina tutte le categorie dalla tabella.
     */
    @Query("DELETE FROM categorie_catalogo")
    suspend fun deleteAll()
}
