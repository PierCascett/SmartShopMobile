package it.unito.smartshopmobile.data.remote

import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product
import retrofit2.Response
import retrofit2.http.GET
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
}

