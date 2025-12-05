/**
 * MainViewModelTest.kt
 *
 * File di test unitari dedicato al MainViewModel (gestione sessione e routing).
 * Documentazione allineata con gli altri test nel modulo.
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
import it.unito.smartshopmobile.domain.UserRole
import it.unito.smartshopmobile.viewModel.CustomerTab
import it.unito.smartshopmobile.viewModel.MainViewModel
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
 * Copre i casi chiave del MainViewModel:
 * - gestione login/logout e salvataggio sessione
 * - restore della sessione da DataStore
 * - routing account in base al ruolo
 * - reset flag UI e tab cliente
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class MainViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val sessionFlow = MutableStateFlow<User?>(null)

    /**
     * Configura i mock prima di ogni test.
     * Mock di SessionDataStore per evitare chiamate reali al DataStore.
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkConstructor(SessionDataStore::class)
        every { anyConstructed<SessionDataStore>().userFlow } returns sessionFlow
        coEvery { anyConstructed<SessionDataStore>().clear() } just runs
        coEvery { anyConstructed<SessionDataStore>().saveUser(any()) } just runs
    }

    /**
     * Pulisce tutti i mock dopo ogni test per evitare interferenze.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test: onLoginSuccess imposta utente, ruolo e messaggio toast.
     */
    @Test
    fun onLoginSuccess_setsUserRoleAndToast() {
        // Arrange: utente simulato che effettua login
        val vm = MainViewModel(context)
        val user = User(1, "Mario", "Rossi", "mario@example.com", ruolo = "cliente")

        // Act: notifichiamo il successo del login
        vm.onLoginSuccess(user, UserRole.CUSTOMER)

        // Assert: stato UI aggiornato con utente, ruolo e messaggio toast
        val state = vm.uiState.value
        assertEquals(user, state.loggedUser)
        assertEquals(UserRole.CUSTOMER, state.selectedRole)
        assertEquals("Accesso: ${user.email}", state.toastMessage)
    }

    /**
     * Helper per gestire on logout clears session and resets state.
     */
    @Test
    fun onLogout_clearsSessionAndResetsState() = runTest {
        // Arrange: simuliamo un utente già loggato
        val vm = MainViewModel(context)
        val user = User(5, "Luisa", "Bianchi", "luisa@example.com", ruolo = "dipendente")
        vm.onLoginSuccess(user, UserRole.EMPLOYEE)

        // Act: chiamiamo logout (svuota DataStore e stato)
        vm.onLogout()
        advanceUntilIdle()

        // Assert: stato riportato ai valori iniziali e clear() invocato
        val state = vm.uiState.value
        assertNull(state.loggedUser)
        assertNull(state.selectedRole)
        assertEquals(CustomerTab.SHOP, state.currentTab)
        assertFalse(state.showCart)
        coVerify(exactly = 1) { anyConstructed<SessionDataStore>().clear() }
    }

    /**
     * Helper per gestire open account for role navigates to correct section.
     */
    @Test
    fun openAccountForRole_navigatesToCorrectSection() {
        // Arrange: ViewModel pronto senza utente
        val vm = MainViewModel(context)

        // Act: chiediamo apertura profilo per i tre ruoli supportati
        vm.openAccountForRole(UserRole.MANAGER)
        val afterManager = vm.uiState.value.managerProfileTrigger

        vm.openAccountForRole(UserRole.EMPLOYEE)
        val afterEmployee = vm.uiState.value.employeeProfileTrigger

        vm.openAccountForRole(UserRole.CUSTOMER)
        val afterCustomerTab = vm.uiState.value.currentTab

        // Assert: trigger incrementati e tab cliente impostata
        assertEquals(1, afterManager)
        assertEquals(1, afterEmployee)
        assertEquals(CustomerTab.ACCOUNT, afterCustomerTab)
    }

    /**
     * Helper per gestire session is restored from data store flow.
     */
    @Test
    fun sessionIsRestoredFromDataStoreFlow() = runTest {
        val user = User(10, "Anna", "Neri", "anna@example.com", ruolo = "cliente")
        sessionFlow.value = user

        val vm = MainViewModel(context)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(user, state.loggedUser)
        assertEquals(it.unito.smartshopmobile.domain.UserRole.CUSTOMER, state.selectedRole)
    }

    /**
     * Test: onLogout resetta tutti i flag e lo stato UI.
     */
    @Test
    fun onLogout_resetsFlagsAndState() = runTest {
        val vm = MainViewModel(context)
        vm.setShowMenu(true)
        vm.setShowCart(true)
        vm.setShowFavorites(true)
        vm.onLoginSuccess(User(1, "Mario", "Rossi", "mario@example.com", ruolo = "cliente"), UserRole.CUSTOMER)

        vm.onLogout()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.loggedUser)
        assertNull(state.selectedRole)
        assertFalse(state.showMenu)
        assertFalse(state.showCart)
        assertFalse(state.showFavorites)
    }
}
