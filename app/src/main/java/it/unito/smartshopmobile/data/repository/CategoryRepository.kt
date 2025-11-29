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

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val apiService: SmartShopApiService
) {
    
    // Ottiene tutte le categorie dal database locale (cache)
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }
    
    // Ottiene le categorie macro dal database locale
    fun getMacroCategories(): Flow<List<Category>> {
        return categoryDao.getMacroCategories()
    }
    
    // Ottiene le sottocategorie dal database locale
    fun getSubcategories(parentId: String): Flow<List<Category>> {
        return categoryDao.getSubcategories(parentId)
    }
    
    // Sincronizza i dati dal server al database locale
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

    private fun Category.toEntity(): Category = this
}
