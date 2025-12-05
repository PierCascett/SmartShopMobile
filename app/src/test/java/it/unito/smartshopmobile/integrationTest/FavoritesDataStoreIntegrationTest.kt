package it.unito.smartshopmobile.integrationTest

import android.content.Context
import it.unito.smartshopmobile.data.datastore.FavoritesDataStore
import it.unito.smartshopmobile.testUtils.FakePreferencesDataStore
import it.unito.smartshopmobile.unitViewModelTest.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Test di integrazione per FavoritesDataStore.
 *
 * Verifica persistenza e isolamento per utente dei preferiti.
 * Utilizza FakePreferencesDataStore per evitare problemi di concorrenza
 * con il DataStore reale su Robolectric/Windows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FavoritesDataStoreIntegrationTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var dataStore: FavoritesDataStore
    private lateinit var fakeDataStore: FakePreferencesDataStore

    @Before
    fun setup() {
        // Crea un'istanza completamente isolata di FakePreferencesDataStore per questo test
        // Ogni test ha la sua istanza in memoria, evitando conflitti
        fakeDataStore = FakePreferencesDataStore()
        val context: Context = RuntimeEnvironment.getApplication()
        dataStore = FavoritesDataStore(context, fakeDataStore)
    }

    @After
    fun tearDown() {
        // Reset esplicito per pulire i dati
        fakeDataStore.reset()
    }

    /**
     * Verifica che il set di preferiti venga salvato e riemerso dal Flow.
     */
    @Test
    fun saveFavorites_emitsPersistedSet() = runTest {
        val favorites = setOf("prd-1", "prd-2", "prd-3")
        dataStore.saveFavorites(userId = 42, favorites = favorites)

        val restored = dataStore.favoritesFlow(userId = 42).first()
        assertEquals(favorites, restored)
    }

    /**
     * Verifica che i preferiti siano isolati per utente e non si sovrascrivano.
     */
    @Test
    fun favorites_areIsolatedPerUser() = runTest {
        val user1 = 10
        val user2 = 11

        dataStore.saveFavorites(userId = user1, favorites = setOf("prd-A"))
        dataStore.saveFavorites(userId = user2, favorites = setOf("prd-B", "prd-C"))

        val restored1 = dataStore.favoritesFlow(userId = user1).first()
        val restored2 = dataStore.favoritesFlow(userId = user2).first()

        assertEquals(setOf("prd-A"), restored1)
        assertEquals(setOf("prd-B", "prd-C"), restored2)
    }
}
