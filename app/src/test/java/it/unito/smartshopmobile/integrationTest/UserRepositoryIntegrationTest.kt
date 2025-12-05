package it.unito.smartshopmobile.integrationTest

import it.unito.smartshopmobile.data.entity.UpdateUserRequest
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import it.unito.smartshopmobile.data.repository.UserRepository
import it.unito.smartshopmobile.unitViewModelTest.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Test di integrazione per UserRepository (update profilo e upload avatar).
 *
 * Verifica interazione end-to-end con MockWebServer per patch profilo
 * e upload multipart, assicurando che i Result contengano i dati attesi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class UserRepositoryIntegrationTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var api: SmartShopApiService
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartShopApiService::class.java)
        repository = UserRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /**
     * Verifica che updateProfile ritorni l'utente aggiornato dal backend.
     */
    @Test
    fun updateProfile_returnsUpdatedUser() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "user": {
                        "id_utente": 5,
                        "nome": "Paolo",
                        "cognome": "Neri",
                        "email": "paolo@example.com",
                        "telefono": "999",
                        "avatar_url": "http://localhost/avatar.jpg",
                        "ruolo": "employee"
                      }
                    }
                    """.trimIndent()
                )
        )

        val request = UpdateUserRequest(
            nome = "Paolo",
            cognome = "Neri",
            email = "paolo@example.com",
            telefono = "999"
        )
        val result = repository.updateProfile(5, request)
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("Paolo", user.nome)
        assertEquals("employee", user.ruolo)
    }

    /**
     * Verifica che uploadAvatar ritorni l'URL fornito dal backend.
     */
    @Test
    fun uploadAvatar_returnsUrl() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"avatarUrl":"http://localhost/avatar-new.jpg"}""")
        )

        val data = ByteArray(4) { 0x01 }
        val result = repository.uploadAvatar(userId = 3, data = data, mimeType = "image/jpeg")

        assertTrue(result.isSuccess)
        assertEquals("http://localhost/avatar-new.jpg", result.getOrThrow())
    }
}
