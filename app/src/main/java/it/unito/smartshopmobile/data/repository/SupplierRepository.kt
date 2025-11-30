/**
 * SupplierRepository.kt
 * 
 * MVVM: Model Layer - Repository fornitori
 * 
 * FUNZIONAMENTO:
 * - Carica elenco fornitori per riordini
 * - Sync da API a Room (cache)
 * - Espone Flow per UI manager
 * 
 * PATTERN MVVM:
 * - Repository Pattern: coordina API + Room
 * - Flow<List<Supplier>>: stream reattivo fornitori
 * - suspend fun: refresh asincrone
 * - Result<T>: gestione errori sync
 */
package it.unito.smartshopmobile.data.repository

import it.unito.smartshopmobile.data.dao.SupplierDao
import it.unito.smartshopmobile.data.entity.Supplier
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow

class SupplierRepository(
    private val apiService: SmartShopApiService,
    private val supplierDao: SupplierDao
) {
    fun observeSuppliers(): Flow<List<Supplier>> = supplierDao.getSuppliers()

    suspend fun refreshSuppliers(): Result<Unit> {
        return try {
            val response = apiService.getSuppliers()
            if (response.isSuccessful && response.body() != null) {
                supplierDao.clearAll()
                supplierDao.insertAll(response.body()!!)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore recupero fornitori (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
