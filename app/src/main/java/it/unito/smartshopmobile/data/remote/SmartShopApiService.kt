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

/**
 * Interface Retrofit per le chiamate API REST al backend SmartShop.
 *
 * Definisce tutti gli endpoint disponibili con annotazioni Retrofit type-safe.
 * Tutte le funzioni sono suspend per integrazione con Kotlin Coroutines.
 * Response<T> permette gestione errori HTTP completa (status code, body, headers).
 *
 * Endpoint principali:
 * - /auth: login e registrazione
 * - /categorie: gestione categorie prodotti
 * - /prodotti: catalogo e ricerca prodotti
 * - /ordini: creazione e gestione ordini
 * - /riordini: riordini magazzino (manager)
 * - /scaffali: scaffali supermercato
 * - /fornitori: fornitori
 * - /magazzino: operazioni inventario
 * - /users: gestione profilo utente
 *
 * Note:
 * - Base URL configurato in RetrofitInstance
 * - Gson converter per JSON serialization
 * - Logging interceptor per debug
 */
interface SmartShopApiService {

    // === CATEGORIE ===

    /**
     * Recupera tutte le categorie prodotti.
     * @return Response con lista di categorie
     */
    @GET("categorie")
    suspend fun getAllCategories(): Response<List<Category>>

    /**
     * Recupera le macro-categorie (parent univoci).
     * @return Response con lista di macro-categorie
     */
    @GET("categorie/macro")
    suspend fun getMacroCategories(): Response<List<Category>>

    /**
     * Recupera le sottocategorie di una categoria parent.
     * @param parentId ID della categoria parent
     * @return Response con lista di sottocategorie
     */
    @GET("categorie/{parentId}/sottocategorie")
    suspend fun getSubcategories(@Path("parentId") parentId: String): Response<List<Category>>

    // === PRODOTTI ===

    /**
     * Recupera tutti i prodotti del catalogo.
     * @return Response con lista di prodotti
     */
    @GET("prodotti")
    suspend fun getAllProducts(): Response<List<Product>>

    /**
     * Recupera i prodotti di una specifica categoria.
     * @param categoryId ID della categoria
     * @return Response con lista di prodotti
     */
    @GET("prodotti/categoria/{categoryId}")
    suspend fun getProductsByCategory(@Path("categoryId") categoryId: String): Response<List<Product>>

    /**
     * Cerca prodotti per nome o marca.
     * @param query Testo da cercare
     * @return Response con lista di prodotti corrispondenti
     */
    @GET("prodotti/search")
    suspend fun searchProducts(@Query("q") query: String): Response<List<Product>>

    // === AUTENTICAZIONE ===

    /**
     * Effettua login con email e password.
     * @param request Map con "email" e "password"
     * @return Response con Map contenente chiave "user"
     */
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): Response<Map<String, User>>

    /**
     * Registra un nuovo utente.
     * @param request Map con "nome", "cognome", "email", "telefono", "password"
     * @return Response con Map contenente chiave "user"
     */
    @POST("auth/register")
    suspend fun register(@Body request: Map<String, String?>): Response<Map<String, User>>

    // === ORDINI ===

    /**
     * Recupera tutti gli ordini.
     * @return Response con lista di ordini completi (con righe)
     */
    @GET("ordini")
    suspend fun getOrders(): Response<List<Order>>

    /**
     * Crea un nuovo ordine.
     * @param request Richiesta con dati ordine e items
     * @return Response con OrderCreated (idOrdine, totale)
     */
    @POST("ordini")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderCreated>

    /**
     * Aggiorna lo stato di un ordine.
     * @param orderId ID dell'ordine da aggiornare
     * @param request Richiesta con nuovo stato
     * @return Response con Order aggiornato
     */
    @PATCH("ordini/{orderId}")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Int,
        @Body request: UpdateOrderStatusRequest
    ): Response<Order>

    // === RIORDINI MAGAZZINO ===

    /**
     * Recupera tutti i riordini magazzino.
     * @return Response con lista di riordini
     */
    @GET("riordini")
    suspend fun getRestocks(): Response<List<Restock>>

    /**
     * Crea un nuovo riordino a fornitore.
     * @param request Richiesta con dati riordino
     * @return Response con Restock creato
     */
    @POST("riordini")
    suspend fun createRestock(@Body request: CreateRestockRequest): Response<Restock>

    // === SCAFFALI E FORNITORI ===

    /**
     * Recupera tutti gli scaffali del supermercato.
     * @return Response con lista di scaffali
     */
    @GET("scaffali")
    suspend fun getAllShelves(): Response<List<Shelf>>

    /**
     * Recupera tutti i fornitori.
     * @return Response con lista di fornitori
     */
    @GET("fornitori")
    suspend fun getSuppliers(): Response<List<Supplier>>

    // === GESTIONE INVENTARIO ===

    /**
     * Trasferisce stock da magazzino a scaffale.
     * @param request Richiesta con prodotto, quantità e scaffale
     * @return Response con StockTransferResult
     */
    @POST("magazzino/trasferisci")
    suspend fun moveStockToShelf(@Body request: StockTransferRequest): Response<StockTransferResult>

    /**
     * Riconcilia arrivi merce da riordini.
     * @return Response con Map di risultati
     */
    @POST("magazzino/riconcilia-arrivi")
    suspend fun reconcileArrivals(): Response<Map<String, Any>>
    /**
     * Helper per gestire update profile.
     */

    @PATCH("auth/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: Int,
        @Body request: UpdateUserRequest
    ): Response<Map<String, User>>
    /**
     * Helper per gestire upload profile photo.
     */

    @Multipart
    @POST("auth/profile/{userId}/photo")
    suspend fun uploadProfilePhoto(
        @Path("userId") userId: Int,
        @Part photo: MultipartBody.Part
    ): Response<Map<String, String>>
}
