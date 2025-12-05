/**
 * ProductDao.kt
 *
 * MVVM: Model Layer - Data Access Object (Room)
 *
 * FUNZIONAMENTO:
 * - Interface per accesso al database Room (SQL abstraito)
 * - Query SQL compilate a compile-time (type-safe)
 * - Espone Flow per reattività (auto-aggiornamento UI)
 * - CRUD operations: insert, query, delete
 *
 * PATTERN MVVM:
 * - DAO Pattern: astrazione accesso database
 * - Flow<List<T>>: stream reattivo (si aggiorna automaticamente)
 * - suspend fun: operazioni write asincrone
 * - Room annotations (@Query, @Insert, @Delete)
 */
package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Product
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object per l'accesso ai prodotti del catalogo nel database Room.
 *
 * Fornisce metodi type-safe per operazioni CRUD sui prodotti, utilizzando
 * query SQL compile-time checked da Room. Espone Flow reattivi per
 * l'osservazione automatica delle modifiche ai dati.
 *
 * Caratteristiche principali:
 * - Query SQL compilate e verificate a compile-time
 * - Flow reattivi che si aggiornano automaticamente
 * - Operazioni asincrone con coroutine (suspend fun)
 * - OnConflictStrategy.REPLACE per sincronizzazione API
 *
 * Nota: productId non è univoco (stesso prodotto può essere su più scaffali),
 * quindi le query by ID restituiscono una riga arbitraria.
 */
@Dao
interface ProductDao {

    /**
     * Osserva tutti i prodotti del catalogo.
     *
     * Il Flow emette automaticamente nuovi valori quando la tabella viene modificata.
     *
     * @return Flow che emette lista di tutti i prodotti
     */
    @Query("SELECT * FROM prodotti_catalogo")
    fun getAllProducts(): Flow<List<Product>>

    /**
     * Osserva i prodotti di una specifica categoria.
     *
     * @param categoryId ID della categoria da filtrare
     * @return Flow che emette lista di prodotti della categoria
     */
    @Query("SELECT * FROM prodotti_catalogo WHERE categoria_id = :categoryId")
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>

    /**
     * Recupera un prodotto specifico per ID (query sincrona).
     *
     * Nota: l'ID prodotto non è univoco (stesso prodotto su più scaffali).
     * Questa query restituisce una riga arbitraria con quell'ID.
     *
     * @param id ID logico del prodotto
     * @return Product se trovato, null altrimenti
     */
    @Query("SELECT * FROM prodotti_catalogo WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    /**
     * Cerca prodotti per nome o marca (ricerca testuale).
     *
     * Utilizza LIKE per ricerca parziale case-insensitive.
     *
     * @param query Testo da cercare in nome o marca
     * @return Flow che emette lista di prodotti corrispondenti
     */
    @Query("SELECT * FROM prodotti_catalogo WHERE nome LIKE '%' || :query || '%' OR marca LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    /**
     * Inserisce o aggiorna una lista di prodotti.
     *
     * REPLACE strategy: se il catalogId esiste già, la riga viene sostituita.
     * Utilizzato per sincronizzare i dati dall'API.
     *
     * @param products Lista di prodotti da inserire/aggiornare
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    /**
     * Elimina tutti i prodotti dalla tabella.
     *
     * Utilizzato per refresh completo dei dati (clear + reinsert).
     */
    @Query("DELETE FROM prodotti_catalogo")
    suspend fun deleteAll()
}
