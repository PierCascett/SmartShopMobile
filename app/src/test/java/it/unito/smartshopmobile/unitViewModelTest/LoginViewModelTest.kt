/**
 * LoginViewModelTest.kt
 *
 * File di test unitari per autenticazione (login/registrazione) nel modulo ViewModel.
 * Mantiene coerente la documentazione dei test con il resto del progetto.
 */
package it.unito.smartshopmobile.unitViewModelTest

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

/**
 * Test unitari per LoginViewModel.
 *
 * Questa classe testa le funzionalità di login e registrazione, inclusi:
 * - Validazione dei campi obbligatori
 * - Gestione del successo del login/registrazione
 * - Persistenza della sessione utente
 * - Gestione degli errori
 *
 * Utilizza MockK per simulare AuthRepository e SessionDataStore.
 * Richiede Robolectric per l'accesso al contesto Android.
 *
 * @property dispatcherRule Regola per configurare il dispatcher di test
 * @property context Contesto dell'applicazione Android di test
 * @property sessionFlow Flow simulato per la sessione utente
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LoginViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val sessionFlow = MutableStateFlow<User?>(null)

    /**
     * Configura i mock prima di ogni test.
     *
     * Inizializza MockK e crea mock per SessionDataStore e AuthRepository.
     * Configura comportamenti di default per evitare chiamate reali alla rete o al database.
     */
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

    /**
     * Pulisce tutti i mock dopo ogni test per evitare interferenze tra test.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test: login con campi mancanti mostra messaggio di validazione.
     *
     * Verifica che il ViewModel mostri un messaggio di errore quando
     * si tenta il login senza inserire email e password.
     */
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

    /**
     * Test: login con successo persiste la sessione ed espone l'utente.
     *
     * Verifica che dopo un login riuscito:
     * - Lo stato di loading sia false
     * - Non ci siano messaggi di errore
     * - L'utente sia esposto tramite loginSuccessUser
     * - La sessione sia stata salvata nel DataStore
     */
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

    /**
     * Test: registrazione con campi obbligatori mancanti restituisce errore.
     *
     * Verifica che il ViewModel mostri immediatamente un errore quando
     * si tenta la registrazione senza compilare i campi obbligatori
     * (nome, cognome, email e password).
     */
    @Test
    fun register_missingMandatoryFields_returnsError() {
        // Arrange: lasciamo vuoti i campi obbligatori
        val vm = LoginViewModel(context)

        // Act: tentiamo la registrazione
        vm.register()

        // Assert: messaggio di errore immediato sui campi mancanti
        assertEquals("Nome, cognome, email e password sono obbligatori", vm.errorMessage.value)
    }

    /**
     * Test: registrazione con successo persiste la sessione ed espone l'utente.
     *
     * Verifica che dopo una registrazione riuscita:
     * - Lo stato di loading sia false
     * - L'utente sia esposto tramite loginSuccessUser
     * - Non ci siano messaggi di errore
     * - La sessione sia stata salvata nel DataStore
     */
    @Test
    fun register_success_persistsSessionAndExposesUser() = runTest {
        // Arrange: configuriamo il mock PRIMA di creare il ViewModel
        val user = User(11, "Test", "User", "new@example.com", ruolo = "cliente")
        coEvery {
            anyConstructed<AuthRepository>().register(
                nome = "Test",
                cognome = "User",
                email = "new@example.com",
                telefono = "123",
                password = "secret"
            )
        } returns Result.success(user)

        val vm = LoginViewModel(context)
        vm.setEmail(user.email)
        vm.setPassword("secret")
        vm.nome.value = "Test"
        vm.cognome.value = "User"
        vm.telefono.value = "123"

        // Act
        vm.register()
        advanceUntilIdle()

        // Assert: stato aggiornato e sessione salvata
        assertFalse(vm.isLoading.value)
        assertEquals(user, vm.loginSuccessUser.value)
        assertNull(vm.errorMessage.value)
        coVerify { anyConstructed<SessionDataStore>().saveUser(user) }
    }
}
