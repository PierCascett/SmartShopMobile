/**
 * CategoryRepository.kt
 * 
 * MVVM: Model Layer - Repository categorie prodotti
 * 
 * FUNZIONAMENTO:
 * - Coordina Room (cache locale) e API (dati remoti)
 * - Gestisce gerarchia categorie (parent/subcategories)
 * - Espone Flow per categorie, macro-categorie, sottocategorie
 * - Pattern Offline-First: legge da Room, sync da API
 * 
 * PATTERN MVVM:
 * - Repository Pattern: astrazione sorgente dati
 * - Flow: stream reattivo categorie
 * - suspend fun: sync asincrone con API
 * - Result<T>: gestione errori type-safe
 */
package it.unito.smartshopmobile.data.repository


import android.util.Log
import it.unito.smartshopmobile.data.dao.CategoryDao
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow

/**
 * Repository per la gestione delle categorie prodotti.
 *
 * Gestisce una struttura gerarchica di categorie (parent/children) con
 * supporto per macro-categorie e sottocategorie. Implementa pattern
 * Offline-First con Room come Single Source of Truth.
 *
 * @property categoryDao DAO per accesso al database locale
 * @property apiService Servizio API per sincronizzazione remota
 */
class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val apiService: SmartShopApiService
) {
    
    /**
     * Osserva tutte le categorie dal database locale.
     *
     * @return Flow che emette lista di tutte le categorie ordinate alfabeticamente
     */
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }
    
    /**
     * Osserva le macro-categorie (categorie parent univoche).
     *
     * @return Flow che emette lista di macro-categorie
     */
    fun getMacroCategories(): Flow<List<Category>> {
        return categoryDao.getMacroCategories()
    }
    
    /**
     * Osserva le sottocategorie di una categoria parent specifica.
     *
     * @param parentId ID della categoria parent
     * @return Flow che emette lista di sottocategorie
     */
    fun getSubcategories(parentId: String): Flow<List<Category>> {
        return categoryDao.getSubcategories(parentId)
    }
    
    /**
     * Sincronizza tutte le categorie dal server al database locale.
     *
     * @return Result<Unit> success se sincronizzazione riuscita
     */
    suspend fun refreshCategories(): Result<Unit> {
        return try {
            val response = apiService.getAllCategories()
            if (response.isSuccessful && response.body() != null) {
                val categories = response.body()!!
                categoryDao.deleteAll()
                categoryDao.insertAll(categories)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore nel caricamento delle categorie: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Errore refresh categorie", e)
            Result.failure(e)
        }
    }
    /**
     * Helper per gestire category.
     */

    private fun Category.toEntity(): Category = this
}
