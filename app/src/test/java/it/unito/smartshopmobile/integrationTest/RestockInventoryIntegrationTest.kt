package it.unito.smartshopmobile.integrationTest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.CreateRestockRequest
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.StockTransferRequest
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import it.unito.smartshopmobile.data.repository.InventoryRepository
import it.unito.smartshopmobile.data.repository.RestockRepository
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
 * Test di integrazione per RestockRepository e InventoryRepository.
 *
 * Copre creazione/fetch dei riordini (Room + API) e le chiamate di inventario
 * (moveStock) includendo la gestione di errori restituiti dal backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RestockInventoryIntegrationTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var api: SmartShopApiService
    private lateinit var db: SmartShopDatabase
    private lateinit var restockRepository: RestockRepository
    private lateinit var inventoryRepository: InventoryRepository

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

        restockRepository = RestockRepository(api, db.restockDao())
        inventoryRepository = InventoryRepository(api)
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    /**
     * Verifica che createRestock restituisca il payload creato e lo inserisca in Room.
     */
    @Test
    fun createRestock_insertsIntoRoom() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id_riordino": 50,
                      "id_prodotto": "prd-10",
                      "prodotto_nome": "Latte",
                      "id_fornitore": 5,
                      "fornitore_nome": "DairyCo",
                      "quantita_ordinata": 30,
                      "data_ordine": "2025-01-02T09:00:00Z",
                      "data_arrivo_prevista": "2025-01-05",
                      "data_arrivo_effettiva": null,
                      "arrivato": false,
                      "id_responsabile": 2,
                      "responsabile_nome": "Mario",
                      "responsabile_cognome": "Rossi"
                    }
                    """.trimIndent()
                )
        )

        val request = CreateRestockRequest(
            idProdotto = "prd-10",
            idFornitore = 5,
            quantitaOrdinata = 30,
            dataArrivoPrevista = "2025-01-05",
            idResponsabile = 2
        )

        val result = restockRepository.createRestock(request)
        assertTrue(result.isSuccess)

        val cached = restockRepository.observeRestocks().first()
        assertEquals(1, cached.size)
        assertEquals("prd-10", cached.first().idProdotto)
    }

    /**
     * Verifica che fetchRestocks sovrascriva la cache locale con il payload API.
     */
    @Test
    fun fetchRestocks_replacesCache() = runTest {
        // cache iniziale
        val seed = Restock(
            idRiordino = 1,
            idProdotto = "old",
            prodottoNome = "Vecchio",
            idFornitore = 1,
            fornitoreNome = "OldSupplier",
            quantitaOrdinata = 5,
            dataOrdine = "2025-01-01T00:00:00Z",
            dataArrivoPrevista = null,
            dataArrivoEffettiva = null,
            arrivato = false,
            idResponsabile = null,
            responsabileNome = null,
            responsabileCognome = null
        )
        db.restockDao().insertAll(listOf(seed))

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id_riordino": 99,
                        "id_prodotto": "prd-new",
                        "prodotto_nome": "Pane",
                        "id_fornitore": 3,
                        "fornitore_nome": "BakeryCo",
                        "quantita_ordinata": 20,
                        "data_ordine": "2025-01-03T12:00:00Z",
                        "data_arrivo_prevista": "2025-01-06",
                        "data_arrivo_effettiva": null,
                        "arrivato": false,
                        "id_responsabile": 4,
                        "responsabile_nome": "Laura",
                        "responsabile_cognome": "Bianchi"
                      }
                    ]
                    """.trimIndent()
                )
        )

        val result = restockRepository.fetchRestocks()
        assertTrue(result.isSuccess)

        val cached = restockRepository.observeRestocks().first()
        assertEquals(1, cached.size)
        assertEquals("prd-new", cached.first().idProdotto)
    }

    /**
     * Verifica che moveStock ritorni successo con quantitativi aggiornati.
     */
    @Test
    fun moveStock_successReturnsResult() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "idProdotto": "prd-10",
                      "idScaffale": 7,
                      "quantitaTrasferita": 5,
                      "magazzinoResiduo": 25,
                      "catalogo": {
                        "idCatalogo": 123,
                        "quantitaDisponibile": 8,
                        "prezzo": 1.99,
                        "vecchioPrezzo": 2.49
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = inventoryRepository.moveStock(
            StockTransferRequest(
                idProdotto = "prd-10",
                quantita = 5,
                idScaffale = 7
            )
        )

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertEquals(25, payload.magazzinoResiduo)
        assertEquals(7, payload.idScaffale)
        assertEquals(8, payload.catalogo.quantitaDisponibile)
    }

    /**
     * Verifica che moveStock propaghi l'errore del backend (JSON "error").
     */
    @Test
    fun moveStock_errorPropagatesMessage() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"Stock insufficiente"}""")
        )

        val result = inventoryRepository.moveStock(
            StockTransferRequest(
                idProdotto = "prd-10",
                quantita = 100,
                idScaffale = 7
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Stock insufficiente") == true)
    }
}
