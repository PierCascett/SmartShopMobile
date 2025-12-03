package it.unito.smartshopmobile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.repository.AuthRepository
import it.unito.smartshopmobile.viewModel.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LoginViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val sessionFlow = MutableStateFlow<User?>(null)

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkConstructor(SessionDataStore::class)
        mockkConstructor(AuthRepository::class)

        every { anyConstructed<SessionDataStore>().userFlow } returns sessionFlow
        coEvery { anyConstructed<SessionDataStore>().saveUser(any()) } just runs
        coEvery { anyConstructed<SessionDataStore>().clear() } just runs

        // Mock di default per AuthRepository per evitare chiamate reali
        coEvery { anyConstructed<AuthRepository>().login(any(), any()) } returns Result.failure(
            Exception("Mock non configurato")
        )
        coEvery { anyConstructed<AuthRepository>().register(any(), any(), any(), any(), any()) } returns Result.failure(
            Exception("Mock non configurato")
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_missingFields_showsValidationMessage() {
        // Arrange: lasciamo i campi vuoti
        val vm = LoginViewModel(context)
        vm.setEmail("")
        vm.setPassword("")

        // Act: eseguiamo login
        vm.login()

        // Assert: errore di validazione mostrato subito
        assertEquals("Inserisci email e password", vm.errorMessage.value)
    }

    @Test
    fun login_success_persistsSessionAndExposesUser() = runTest {
        // Arrange: configuriamo il mock PRIMA di creare il ViewModel
        val user = User(10, "Test", "User", "test@example.com", ruolo = "cliente")
        coEvery { anyConstructed<AuthRepository>().login("test@example.com", "password") } returns Result.success(user)

        val vm = LoginViewModel(context)
        vm.setEmail(user.email)
        vm.setPassword("password")

        // Act: avviamo il login (coroutine di rete simulata)
        vm.login()
        advanceUntilIdle()

        // Assert: stato aggiornato, nessun errore e sessione salvata
        assertFalse(vm.isLoading.value)
        assertNull(vm.errorMessage.value)
        assertEquals(user, vm.loginSuccessUser.value)
        coVerify { anyConstructed<SessionDataStore>().saveUser(user) }
    }

    @Test
    fun register_missingMandatoryFields_returnsError() {
        // Arrange: lasciamo vuoti i campi obbligatori
        val vm = LoginViewModel(context)

        // Act: tentiamo la registrazione
        vm.register()

        // Assert: messaggio di errore immediato sui campi mancanti
        assertEquals("Nome, cognome, email e password sono obbligatori", vm.errorMessage.value)
    }
}
