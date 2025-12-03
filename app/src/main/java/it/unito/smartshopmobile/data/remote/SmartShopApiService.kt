/**
 * SmartShopApiService.kt
 *
 * MVVM: Model Layer - Interface API REST (Retrofit)
 *
 * FUNZIONAMENTO:
 * - Definisce endpoint API backend Node.js
 * - Annotazioni Retrofit: @GET, @POST, @PATCH, @Multipart
 * - suspend fun: integrazione Coroutines
 * - Response<T>: wrapper per gestione errori HTTP
 * - Endpoint: auth, products, orders, restocks, inventory, users
 *
 * PATTERN MVVM:
 * - API interface: contract REST API
 * - Retrofit annotations: mapping endpoint
 * - suspend fun: chiamate asincrone
 * - Type-safe: errori compile-time
 */
package it.unito.smartshopmobile.data.remote


import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.CreateOrderRequest
import it.unito.smartshopmobile.data.entity.CreateRestockRequest
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.OrderCreated
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.Shelf
import it.unito.smartshopmobile.data.entity.StockTransferRequest
import it.unito.smartshopmobile.data.entity.StockTransferResult
import it.unito.smartshopmobile.data.entity.Supplier
import it.unito.smartshopmobile.data.entity.UpdateOrderStatusRequest
import it.unito.smartshopmobile.data.entity.UpdateUserRequest
import it.unito.smartshopmobile.data.entity.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface SmartShopApiService {

    @GET("categorie")
    suspend fun getAllCategories(): Response<List<Category>>

    @GET("categorie/macro")
    suspend fun getMacroCategories(): Response<List<Category>>

    @GET("categorie/{parentId}/sottocategorie")
    suspend fun getSubcategories(@Path("parentId") parentId: String): Response<List<Category>>

    @GET("prodotti")
    suspend fun getAllProducts(): Response<List<Product>>

    @GET("prodotti/categoria/{categoryId}")
    suspend fun getProductsByCategory(@Path("categoryId") categoryId: String): Response<List<Product>>

    @GET("prodotti/search")
    suspend fun searchProducts(@Query("q") query: String): Response<List<Product>>

    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): Response<Map<String, User>>

    @POST("auth/register")
    suspend fun register(@Body request: Map<String, String?>): Response<Map<String, User>>

    @GET("ordini")
    suspend fun getOrders(): Response<List<Order>>

    @POST("ordini")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderCreated>

    @PATCH("ordini/{orderId}")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Int,
        @Body request: UpdateOrderStatusRequest
    ): Response<Order>

    @GET("riordini")
    suspend fun getRestocks(): Response<List<Restock>>

    @POST("riordini")
    suspend fun createRestock(@Body request: CreateRestockRequest): Response<Restock>

    @GET("scaffali")
    suspend fun getAllShelves(): Response<List<Shelf>>

    @GET("fornitori")
    suspend fun getSuppliers(): Response<List<Supplier>>

    @POST("magazzino/trasferisci")
    suspend fun moveStockToShelf(@Body request: StockTransferRequest): Response<StockTransferResult>

    @POST("magazzino/riconcilia-arrivi")
    suspend fun reconcileArrivals(): Response<Map<String, Any>>

    @PATCH("auth/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: Int,
        @Body request: UpdateUserRequest
    ): Response<Map<String, User>>

    @Multipart
    @POST("auth/profile/{userId}/photo")
    suspend fun uploadProfilePhoto(
        @Path("userId") userId: Int,
        @Part photo: MultipartBody.Part
    ): Response<Map<String, String>>
}
