/**
 * RestockRepository.kt
 * 
 * MVVM: Model Layer - Repository riordini magazzino
 * 
 * FUNZIONAMENTO:
 * - Crea riordini a fornitori (manager)
 * - Osserva storico riordini con stato
 * - Sync da API a Room per cache locale
 * 
 * PATTERN MVVM:
 * - Repository Pattern: coordina API + Room
 * - Flow<List<Restock>>: stream reattivo riordini
 * - suspend fun: create/fetch asincrone
 * - Result<T>: gestione errori
 */
package it.unito.smartshopmobile.data.repository


import android.util.Log
import it.unito.smartshopmobile.data.dao.RestockDao
import it.unito.smartshopmobile.data.entity.CreateRestockRequest
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow

class RestockRepository(
    private val apiService: SmartShopApiService,
    private val restockDao: RestockDao
) {
    fun observeRestocks(): Flow<List<Restock>> = restockDao.getRestocks()

    suspend fun fetchRestocks(): Result<Unit> {
        return try {
            val response = apiService.getRestocks()
            if (response.isSuccessful) {
                val items = response.body().orEmpty()
                restockDao.deleteAll()
                restockDao.insertAll(items)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore recupero riordini (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("RestockRepository", "Errore fetch riordini", e)
            Result.failure(e)
        }
    }

    suspend fun createRestock(request: CreateRestockRequest): Result<Restock> {
        return try {
            val response = apiService.createRestock(request)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                // aggiorna cache locale aggiungendo il nuovo riordino
                restockDao.insertAll(listOf(created))
                Result.success(created)
            } else {
                Result.failure(Exception("Errore creazione riordino (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("RestockRepository", "Errore create riordino", e)
            Result.failure(e)
        }
    }
}
