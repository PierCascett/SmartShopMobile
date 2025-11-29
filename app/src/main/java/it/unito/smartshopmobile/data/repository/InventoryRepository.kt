/**
 * InventoryRepository.kt
 * 
 * MVVM: Model Layer - Repository gestione inventario
 * 
 * FUNZIONAMENTO:
 * - Trasferisce stock da magazzino a scaffali (manager)
 * - Riconcilia arrivi merce da riordini
 * - Solo API (no cache locale)
 * 
 * PATTERN MVVM:
 * - Repository Pattern: astrazione API inventario
 * - suspend fun: operazioni asincrone
 * - Result<T>: gestione errori trasferimenti
 * - API-only: modifiche immediate server
 */
package it.unito.smartshopmobile.data.repository


import android.util.Log
import it.unito.smartshopmobile.data.entity.StockTransferRequest
import it.unito.smartshopmobile.data.entity.StockTransferResult
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import org.json.JSONObject

class InventoryRepository(
    private val apiService: SmartShopApiService
) {
    suspend fun moveStock(request: StockTransferRequest): Result<StockTransferResult> {
        return try {
            val response = apiService.moveStockToShelf(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val message = response.errorBody()?.string()
                val parsed = parseError(message)
                Result.failure(
                    Exception(parsed ?: "Errore trasferimento scorte (${response.code()})")
                )
            }
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Errore trasferimento scorte", e)
            Result.failure(e)
        }
    }

    suspend fun reconcileArrivals(): Result<Unit> {
        return try {
            val response = apiService.reconcileArrivals()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val message = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        message?.takeIf { it.isNotBlank() }
                            ?: "Errore riconciliazione magazzino (${response.code()})"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Errore riconciliazione magazzino", e)
            Result.failure(e)
        }
    }

    private fun parseError(raw: String?): String? = raw?.let {
        try {
            JSONObject(it).optString("error").takeIf { msg -> msg.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
