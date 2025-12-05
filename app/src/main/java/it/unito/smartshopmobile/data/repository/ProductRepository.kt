/**
 * ProductRepository.kt
 *
 * MVVM: Model Layer - Repository prodotti
 *
 * FUNZIONAMENTO:
 * - Coordina dati locali (Room) e remoti (Retrofit API)
 * - Espone Flow<List<Product>> per reattività
 * - Sincronizza prodotti da server a database locale
 * - Pattern "Offline-First": legge da Room, sincronizza da API
 *
 * PATTERN MVVM:
 * - Repository Pattern: astrazione sorgente dati
 * - Flow: stream reattivo di dati (osservabile)
 * - suspend fun: operazioni asincrone con Coroutines
 * - Result<T>: gestione errori type-safe
 */
package it.unito.smartshopmobile.data.repository

import android.util.Log
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow

/**
 * Repository per la gestione dei prodotti del catalogo.
 *
 * Implementa il pattern Offline-First: espone dati dal database locale (Room)
 * tramite Flow reattivi e sincronizza periodicamente con il backend API.
 * I ViewModel osservano i Flow e ricevono automaticamente gli aggiornamenti.
 *
 * Caratteristiche principali:
 * - Single Source of Truth: Room database
 * - Flow reattivi per osservazione automatica delle modifiche
 * - Sincronizzazione API → Room per aggiornamenti
 * - Operazioni di ricerca e filtraggio locale
 *
 * Pattern di utilizzo:
 * ```kotlin
 * // Osserva prodotti (reattivo)
 * productRepository.getAllProducts().collect { products ->
 *     // UI si aggiorna automaticamente
 * }
 *
 * // Sincronizza da API
 * productRepository.refreshProducts().fold(
 *     onSuccess = { /* sync ok */ },
 *     onFailure = { /* errore rete */ }
 * )
 * ```
 *
 * @property productDao DAO per accesso al database locale
 * @property apiService Servizio API per sincronizzazione remota
 */
class ProductRepository(
    private val productDao: ProductDao,
    private val apiService: SmartShopApiService
) {

    /**
     * Osserva tutti i prodotti dal database locale.
     *
     * Il Flow emette automaticamente nuovi valori quando i prodotti
     * nel database vengono modificati (es. dopo refresh da API).
     *
     * @return Flow che emette lista di tutti i prodotti
     */
    fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts()
    }

    /**
     * Osserva i prodotti di una specifica categoria dal database locale.
     *
     * @param categoryId ID della categoria da filtrare
     * @return Flow che emette lista di prodotti della categoria
     */
    fun getProductsByCategory(categoryId: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(categoryId)
    }

    /**
     * Cerca prodotti per nome o marca nel database locale.
     *
     * @param query Testo da cercare
     * @return Flow che emette lista di prodotti corrispondenti
     */
    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query)
    }

    /**
     * Sincronizza tutti i prodotti dal server al database locale.
     *
     * Operazione: DELETE all + INSERT all (refresh completo).
     * I Flow attivi si aggiorneranno automaticamente dopo l'inserimento.
     *
     * @return Result<Unit> success se sincronizzazione riuscita, failure altrimenti
     */
    suspend fun refreshProducts(): Result<Unit> {
        return try {
            // Fetch da API
            val response = apiService.getAllProducts()
            if (response.isSuccessful && response.body() != null) {
                val products = response.body()!!.map { it.toEntity() }
                // Refresh completo: clear + insert
                productDao.deleteAll()
                productDao.insertAll(products)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore nel caricamento dei prodotti: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Errore refresh prodotti", e)
            Result.failure(e)
        }
    }

    /**
     * Sincronizza i prodotti di una specifica categoria dal server.
     *
     * Inserisce/aggiorna solo i prodotti della categoria specificata.
     * Strategia REPLACE: sovrascrive prodotti esistenti.
     *
     * @param categoryId ID della categoria da sincronizzare
     * @return Result<Unit> success se sincronizzazione riuscita, failure altrimenti
     */
    suspend fun refreshProductsByCategory(categoryId: String): Result<Unit> {
        return try {
            val response = apiService.getProductsByCategory(categoryId)
            if (response.isSuccessful && response.body() != null) {
                val products = response.body()!!.map { it.toEntity() }
                productDao.insertAll(products)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore nel caricamento prodotti categoria: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Errore refresh prodotti categoria", e)
            Result.failure(e)
        }
    }
    /**
     * Helper per gestire product.
     */

    private fun Product.toEntity(): Product = this
}
