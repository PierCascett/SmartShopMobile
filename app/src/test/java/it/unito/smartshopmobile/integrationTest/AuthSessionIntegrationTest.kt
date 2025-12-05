package it.unito.smartshopmobile.integrationTest

import android.content.Context
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import it.unito.smartshopmobile.data.repository.AuthRepository
import it.unito.smartshopmobile.testUtils.FakePreferencesDataStore
import it.unito.smartshopmobile.unitViewModelTest.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Test di integrazione per AuthRepository e SessionDataStore.
 *
 * Usa MockWebServer per simulare il backend di autenticazione e verifica
 * che i dati utente restituiti vengano salvati correttamente nel DataStore.
 *
 * Utilizza FakePreferencesDataStore per evitare problemi di concorrenza
 * con il DataStore reale su Robolectric/Windows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AuthSessionIntegrationTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var api: SmartShopApiService
    private lateinit var repository: AuthRepository
    private lateinit var sessionDataStore: SessionDataStore
    private lateinit var fakeDataStore: FakePreferencesDataStore

    @Before
    fun setup() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartShopApiService::class.java)

        repository = AuthRepository(api)

        // Crea un'istanza completamente isolata di FakePreferencesDataStore per questo test
        // Ogni test ha la sua istanza in memoria, evitando conflitti
        fakeDataStore = FakePreferencesDataStore()
        val context: Context = RuntimeEnvironment.getApplication()
        sessionDataStore = SessionDataStore(context, fakeDataStore)
    }

    @After
    fun tearDown() {
        server.shutdown()
        // Reset esplicito per pulire i dati
        fakeDataStore.reset()
    }

    /**
     * Verifica che il login restituisca l'utente dal backend
     * e che il salvataggio nel DataStore renda l'utente recuperabile via Flow.
     */
    @Test
    fun login_persistsUserInSessionDataStore() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "user": {
                        "id_utente": 7,
                        "nome": "Luca",
                        "cognome": "Bianchi",
                        "email": "luca@example.com",
                        "telefono": "12345",
                        "avatar_url": "http://localhost/img.png",
                        "ruolo": "customer"
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = repository.login("luca@example.com", "password")
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()

        sessionDataStore.saveUser(user)
        val restored = sessionDataStore.userFlow.first()
        assertEquals(7, restored?.id)
        assertEquals("Luca", restored?.nome)
        assertEquals("customer", restored?.ruolo)
    }

    /**
     * Verifica che la chiamata a clear rimuova la sessione e che il Flow emetta null.
     */
    @Test
    fun clearSession_removesUser() = runTest {
        val fakeUser = User(
            id = 9,
            nome = "Marta",
            cognome = "Verdi",
            email = "marta@example.com",
            telefono = null,
            avatarUrl = null,
            ruolo = "manager"
        )
        sessionDataStore.saveUser(fakeUser)

        sessionDataStore.clear()
        val restored = sessionDataStore.userFlow.first()
        assertNull(restored)
    }
}
