package it.unito.smartshopmobile.integrationTest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.unitViewModelTest.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Test di integrazione per repository Catalogo/Categorie.
 *
 * Verifica il percorso end-to-end API fake (MockWebServer) -> Retrofit -> Repository -> Room
 * assicurando che i Flow esposti riflettano i dati restituiti dal backend simulato.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RepositoryIntegrationTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var api: SmartShopApiService
    private lateinit var db: SmartShopDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var productRepository: ProductRepository

    @Before
    fun setup() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartShopApiService::class.java)

        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SmartShopDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        categoryRepository = CategoryRepository(db.categoryDao(), api)
        productRepository = ProductRepository(db.productDao(), api)
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    /**
     * Verifica che il refresh delle categorie scarichi il payload API,
     * popoli Room e renda disponibili i dati tramite Flow.
     */
    @Test
    fun refreshCategories_populatesRoomAndFlows() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {"id":"cat-1","nome":"Latticini","descrizione":"Reparto latte","parentId":null,"parentName":null,"prodottiTotali":2},
                      {"id":"cat-2","nome":"Detersivi","descrizione":"Casa","parentId":null,"parentName":null,"prodottiTotali":1}
                    ]
                    """.trimIndent()
                )
        )

        val result = categoryRepository.refreshCategories()
        assertTrue(result.isSuccess)

        val categories = categoryRepository.getAllCategories().first()
        assertEquals(2, categories.size)
        val first = categories.first { it.id == "cat-1" }
        assertEquals("Latticini", first.nome)
        assertEquals(2, first.prodottiTotali)
    }

    /**
     * Verifica che il refresh dei prodotti sovrascriva i dati locali
     * con il payload dell'API e che i Flow riflettano il nuovo contenuto.
     */
    @Test
    fun refreshProducts_replacesLocalDataWithApiPayload() = runTest {
        val baseCategory = Category(
            id = "cat-1",
            nome = "Latticini",
            descrizione = "Reparto latte",
            parentId = null,
            parentName = null,
            prodottiTotali = 10
        )
        db.categoryDao().insertAll(listOf(baseCategory))
        db.productDao().insertAll(
            listOf(
                Product(
                    catalogId = 99,
                    id = "old",
                    name = "Vecchio prodotto",
                    brand = "OldBrand",
                    categoryId = baseCategory.id,
                    categoryName = baseCategory.nome,
                    categoryDescription = baseCategory.descrizione,
                    catalogQuantity = 1,
                    warehouseQuantity = 1,
                    totalQuantity = 2,
                    price = 0.99,
                    oldPrice = null,
                    availability = "OK",
                    tags = listOf("old"),
                    description = "old",
                    imageUrl = null,
                    shelfId = 1
                )
            )
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "catalogId":1,
                        "id":"prd-1",
                        "name":"Latte Intero",
                        "brand":"Parmalat",
                        "categoryId":"cat-1",
                        "categoryName":"Latticini",
                        "categoryDescription":"Reparto latte",
                        "catalogQuantity":5,
                        "warehouseQuantity":10,
                        "totalQuantity":15,
                        "price":1.79,
                        "oldPrice":2.10,
                        "availability":"OK",
                        "tags":["bio","latte"],
                        "description":"Latte fresco",
                        "imageUrl":"http://localhost/images/latte.png",
                        "shelfId":3
                      }
                    ]
                    """.trimIndent()
                )
        )

        val result = productRepository.refreshProducts()
        assertTrue(result.isSuccess)

        val products = productRepository.getAllProducts().first()
        assertEquals(1, products.size)
        val product = products.first()
        assertEquals("prd-1", product.id)
        assertEquals(15, product.totalQuantity)
        assertEquals(listOf("bio", "latte"), product.tags)
        assertEquals("Parmalat", product.brand)
    }

    /**
     * Verifica che il Flow filtrato per categoria rifletta i prodotti della categoria dopo il refresh.
     */
    @Test
    fun refreshProducts_filtersByCategoryFlow() = runTest {
        val cat1 = Category(id = "cat-1", nome = "Latticini", descrizione = null, parentId = null, parentName = null, prodottiTotali = 0)
        val cat2 = Category(id = "cat-2", nome = "Bevande", descrizione = null, parentId = null, parentName = null, prodottiTotali = 0)
        db.categoryDao().insertAll(listOf(cat1, cat2))

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "catalogId":1,
                        "id":"prd-1",
                        "name":"Latte",
                        "brand":"Parmalat",
                        "categoryId":"cat-1",
                        "categoryName":"Latticini",
                        "categoryDescription":null,
                        "catalogQuantity":5,
                        "warehouseQuantity":10,
                        "totalQuantity":15,
                        "price":1.79,
                        "oldPrice":null,
                        "availability":"OK",
                        "tags":["bio"],
                        "description":"Latte fresco",
                        "imageUrl":null,
                        "shelfId":1
                      },
                      {
                        "catalogId":2,
                        "id":"prd-2",
                        "name":"Acqua",
                        "brand":"BrandA",
                        "categoryId":"cat-2",
                        "categoryName":"Bevande",
                        "categoryDescription":null,
                        "catalogQuantity":20,
                        "warehouseQuantity":30,
                        "totalQuantity":50,
                        "price":0.5,
                        "oldPrice":null,
                        "availability":"OK",
                        "tags":[],
                        "description":"Acqua",
                        "imageUrl":null,
                        "shelfId":2
                      }
                    ]
                    """.trimIndent()
                )
        )

        val result = productRepository.refreshProducts()
        assertTrue(result.isSuccess)

        val cat1Products = productRepository.getProductsByCategory("cat-1").first()
        val cat2Products = productRepository.getProductsByCategory("cat-2").first()

        assertEquals(1, cat1Products.size)
        assertEquals("prd-1", cat1Products.first().id)
        assertEquals(1, cat2Products.size)
        assertEquals("prd-2", cat2Products.first().id)
    }
}
