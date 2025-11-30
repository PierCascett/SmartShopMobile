/**
 * ShelfRepository.kt
 * 
 * MVVM: Model Layer - Repository scaffali magazzino
 * 
 * FUNZIONAMENTO:
 * - Carica scaffali per mappa interattiva (picking)
 * - Sync da API a Room (cache locale)
 * - Espone Flow per UI mappa dipendente
 * 
 * PATTERN MVVM:
 * - Repository Pattern: coordina API + Room
 * - Flow<List<Shelf>>: stream reattivo scaffali
 * - suspend fun: refresh asincrone
 * - Result<T>: gestione errori sync
 */
package it.unito.smartshopmobile.data.repository


import android.util.Log
import it.unito.smartshopmobile.data.dao.ShelfDao
import it.unito.smartshopmobile.data.entity.Shelf
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow

class ShelfRepository(
    private val shelfDao: ShelfDao,
    private val apiService: SmartShopApiService
) {
    fun getAll(): Flow<List<Shelf>> = shelfDao.getAll()

    suspend fun refresh(): Result<Unit> {
        return try {
            val response = apiService.getAllShelves()
            if (response.isSuccessful && response.body() != null) {
                shelfDao.deleteAll()
                shelfDao.insertAll(response.body()!!)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore caricamento scaffali (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("ShelfRepository", "Errore refresh scaffali", e)
            Result.failure(e)
        }
    }
}

