package it.unito.smartshopmobile.integrationTest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.OrderWithLines
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import it.unito.smartshopmobile.data.repository.OrderRepository
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
 * Test di integrazione per OrderRepository.
 *
 * Copre il flusso completo API (MockWebServer) -> Retrofit -> OrderRepository -> Room,
 * verificando che ordini e righe vengano sincronizzati e che gli update di stato
 * propaghino il nuovo valore nella cache locale osservabile.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OrderRepositoryIntegrationTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var api: SmartShopApiService
    private lateinit var db: SmartShopDatabase
    private lateinit var repository: OrderRepository

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

        repository = OrderRepository(api, db.orderDao())
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    /**
     * Verifica che refreshOrders scarichi ordini + righe dal backend
     * e li inserisca correttamente in Room (Flow di OrderWithLines).
     */
    @Test
    fun refreshOrders_populatesOrdersAndLines() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id_ordine":10,
                        "id_utente":1,
                        "data_ordine":"2025-01-01T10:00:00Z",
                        "stato":"IN_LAVORAZIONE",
                        "totale":25.5,
                        "metodo_consegna":"LOCKER",
                        "id_locker":5,
                        "codice_ritiro":"ABC123",
                        "indirizzo_spedizione":null,
                        "nome":"Mario",
                        "cognome":"Rossi",
                        "email":"mario.rossi@example.com",
                        "righe":[
                          {
                            "id_riga":100,
                            "id_ordine":10,
                            "id_prodotto":"prd-1",
                            "quantita":2,
                            "prezzo_unitario":3.5,
                            "prezzo_totale":7.0,
                            "nome":"Detersivo",
                            "marca":"BrandX"
                          },
                          {
                            "id_riga":101,
                            "id_ordine":10,
                            "id_prodotto":"prd-2",
                            "quantita":1,
                            "prezzo_unitario":18.5,
                            "prezzo_totale":18.5,
                            "nome":"Spazzolino",
                            "marca":"BrandY"
                          }
                        ]
                      }
                    ]
                    """.trimIndent()
                )
        )

        val result = repository.refreshOrders()
        assertTrue(result.isSuccess)

        val ordersWithLines: List<OrderWithLines> = repository.observeOrders().first()
        assertEquals(1, ordersWithLines.size)
        val order = ordersWithLines.first()
        assertEquals(10, order.order.idOrdine)
        assertEquals("IN_LAVORAZIONE", order.order.stato)
        assertEquals(2, order.lines.size)
        assertEquals("prd-1", order.lines.first().idProdotto)
        assertEquals(18.5, order.lines.last().prezzoUnitario, 0.0)
    }

    /**
     * Verifica che updateOrderStatus invochi l'endpoint PATCH e poi ricarichi
     * gli ordini aggiornando lo stato nella cache locale.
     */
    @Test
    fun updateOrderStatus_refreshesOrdersWithNewState() = runTest {
        // PATCH aggiornamento stato
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id_ordine":10,
                      "id_utente":1,
                      "data_ordine":"2025-01-01T10:00:00Z",
                      "stato":"CONSEGNATO",
                      "totale":25.5,
                      "metodo_consegna":"LOCKER",
                      "id_locker":5,
                      "codice_ritiro":"ABC123",
                      "indirizzo_spedizione":null,
                      "nome":"Mario",
                      "cognome":"Rossi",
                      "email":"mario.rossi@example.com",
                      "righe":[]
                    }
                    """.trimIndent()
                )
        )
        // GET ordini dopo l'update
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id_ordine":10,
                        "id_utente":1,
                        "data_ordine":"2025-01-01T10:00:00Z",
                        "stato":"CONSEGNATO",
                        "totale":25.5,
                        "metodo_consegna":"LOCKER",
                        "id_locker":5,
                        "codice_ritiro":"ABC123",
                        "indirizzo_spedizione":null,
                        "nome":"Mario",
                        "cognome":"Rossi",
                        "email":"mario.rossi@example.com",
                        "righe":[]
                      }
                    ]
                    """.trimIndent()
                )
        )

        val result = repository.updateOrderStatus(orderId = 10, stato = "CONSEGNATO")
        assertTrue(result.isSuccess)

        val ordersWithLines = repository.observeOrders().first()
        assertEquals(1, ordersWithLines.size)
        assertEquals("CONSEGNATO", ordersWithLines.first().order.stato)
    }
}
