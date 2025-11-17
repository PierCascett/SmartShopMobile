package it.unito.smartshopmobile.data.repository

import android.util.Log
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val apiService: SmartShopApiService
) {

    // Ottiene tutti i prodotti dal database locale
    fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts()
    }

    // Ottiene i prodotti per categoria dal database locale
    fun getProductsByCategory(categoryId: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(categoryId)
    }

    // Cerca prodotti nel database locale
    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query)
    }

    // Sincronizza tutti i prodotti dal server
    suspend fun refreshProducts(): Result<Unit> {
        return try {
            val response = apiService.getAllProducts()
            if (response.isSuccessful && response.body() != null) {
                val products = response.body()!!.map { it.toEntity() }
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

    // Sincronizza i prodotti di una specifica categoria
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

    private fun Product.toEntity(): Product = this
}
