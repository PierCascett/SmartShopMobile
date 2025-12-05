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

/**
 * Repository per la gestione dei fornitori.
 *
 * Fornisce accesso all'elenco fornitori utilizzato dal manager per creare
 * riordini di merce. Implementa pattern Offline-First con cache Room.
 *
 * @property apiService Servizio API per sincronizzazione remota
 * @property supplierDao DAO per cache locale fornitori
 */
class SupplierRepository(
    private val apiService: SmartShopApiService,
    private val supplierDao: SupplierDao
) {
    /**
     * Osserva tutti i fornitori dal database locale.
     *
     * @return Flow che emette lista di fornitori ordinati alfabeticamente
     */
    fun observeSuppliers(): Flow<List<Supplier>> = supplierDao.getSuppliers()

    /**
     * Sincronizza fornitori dal server al database locale.
     *
     * @return Result<Unit> success se sincronizzazione riuscita
     */
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
