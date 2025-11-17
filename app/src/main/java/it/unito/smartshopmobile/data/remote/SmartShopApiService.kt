package it.unito.smartshopmobile.data.remote

import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.CreateOrderRequest
import it.unito.smartshopmobile.data.entity.OrderCreated
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.CreateRestockRequest
import it.unito.smartshopmobile.data.entity.Shelf
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST

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

    @GET("riordini")
    suspend fun getRestocks(): Response<List<Restock>>

    @POST("riordini")
    suspend fun createRestock(@Body request: CreateRestockRequest): Response<Restock>

    @GET("scaffali")
    suspend fun getAllShelves(): Response<List<Shelf>>
}
