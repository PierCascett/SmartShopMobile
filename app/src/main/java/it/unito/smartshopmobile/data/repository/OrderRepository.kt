package it.unito.smartshopmobile.data.repository

import android.util.Log
import it.unito.smartshopmobile.data.dao.OrderDao
import it.unito.smartshopmobile.data.entity.CreateOrderRequest
import it.unito.smartshopmobile.data.entity.OrderCreated
import it.unito.smartshopmobile.data.entity.OrderWithLines
import it.unito.smartshopmobile.data.entity.UpdateOrderStatusRequest
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class OrderRepository(
    private val apiService: SmartShopApiService,
    private val orderDao: OrderDao
) {
    fun observeOrders(): Flow<List<OrderWithLines>> = orderDao.getOrdersWithLines()

    suspend fun createOrder(request: CreateOrderRequest): Result<OrderCreated> {
        return try {
            val response = apiService.createOrder(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Errore creazione ordine (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Errore create order", e)
            Result.failure(e)
        }
    }

    suspend fun refreshOrders(): Result<Unit> {
        return try {
            val response = apiService.getOrders()
            if (response.isSuccessful && response.body() != null) {
                val orders = response.body()!!
                // separo righe
                val lines = orders.flatMap { order ->
                    order.righe.map { it.copy(idOrdine = order.idOrdine) }
                }
                // pulizia + insert
                orderDao.deleteAllLines()
                orderDao.deleteAllOrders()
                orderDao.insertOrders(orders.map { it.copy(righe = emptyList()) })
                orderDao.insertLines(lines)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore nel recupero ordini (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Errore fetch ordini", e)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: Int, stato: String): Result<Unit> {
        return try {
            val response = apiService.updateOrderStatus(
                orderId = orderId,
                request = UpdateOrderStatusRequest(stato = stato)
            )
            if (response.isSuccessful) {
                // Risincronizza la cache locale per riflettere il nuovo stato
                refreshOrders()
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string()
                val parsed = errorMsg?.let {
                    try {
                        JSONObject(it).optString("error").takeIf { msg -> msg.isNotBlank() }
                            ?: JSONObject(it).optString("message").takeIf { msg -> msg.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    }
                }
                Result.failure(
                    Exception(
                        parsed?.takeIf { it.isNotBlank() }
                            ?: "Errore aggiornamento ordine (${response.code()})"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Errore update order", e)
            Result.failure(e)
        }
    }
}
