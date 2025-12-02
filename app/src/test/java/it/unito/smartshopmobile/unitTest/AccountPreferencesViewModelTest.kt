package it.unito.smartshopmobile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.datastore.AccountPreferencesDataStore
import it.unito.smartshopmobile.viewModel.AccountPreferencesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AccountPreferencesViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val prefsFlow = MutableStateFlow(AccountPreferences())

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkConstructor(AccountPreferencesDataStore::class)

        every { anyConstructed<AccountPreferencesDataStore>().data } returns prefsFlow
        coEvery {
            anyConstructed<AccountPreferencesDataStore>().updateProfile(any(), any(), any(), any())
        } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun updateProfile_callsDataStoreCorrectly() = runTest {
        // Arrange: ViewModel collegato al flow fake
        val vm = AccountPreferencesViewModel(context)

        // Act: aggiorniamo i dati del profilo
        vm.updateProfile("Anna", "Verdi", "Via Roma 10", "3331234567")
        advanceUntilIdle()

        // Assert: verifica che il DataStore sia stato chiamato con i parametri corretti
        coVerify(exactly = 1) {
            anyConstructed<AccountPreferencesDataStore>().updateProfile(
                "Anna",
                "Verdi",
                "Via Roma 10",
                "3331234567"
            )
        }
    }


    @Test
    fun defaultPreferences_areEmptyWhenDatastoreEmpty() {
        // Arrange: prefsFlow parte vuoto (valori di default)
        val vm = AccountPreferencesViewModel(context)

        // Assert: i valori di default sono stringhe vuote
        val prefs = vm.preferences.value
        assertEquals("", prefs.nome)
        assertEquals("", prefs.cognome)
        assertEquals("", prefs.indirizzoSpedizione)
        assertEquals("", prefs.telefono)
    }

    @Test
    fun preferences_reactsToDataStoreUpdates() = runTest {
        // Arrange: ViewModel creato con dati iniziali vuoti
        val vm = AccountPreferencesViewModel(context)

        // Attiva il collector per mantenere il StateFlow attivo
        val job = backgroundScope.launch {
            vm.preferences.collect { /* mantiene attivo WhileSubscribed */ }
        }
        advanceUntilIdle()

        // Assert: valore iniziale vuoto
        assertEquals("", vm.preferences.value.nome)

        // Act: il DataStore emette nuovi valori (simula aggiornamento esterno)
        prefsFlow.value = AccountPreferences(
            nome = "Luigi",
            cognome = "Verdi",
            indirizzoSpedizione = "Corso Italia 45",
            telefono = "3339876543"
        )
        advanceUntilIdle()

        // Assert: il ViewModel espone i nuovi valori
        val updatedPrefs = vm.preferences.value
        assertEquals("Luigi", updatedPrefs.nome)
        assertEquals("Verdi", updatedPrefs.cognome)
        assertEquals("Corso Italia 45", updatedPrefs.indirizzoSpedizione)
        assertEquals("3339876543", updatedPrefs.telefono)

        job.cancel()
    }
}
