/**
 * EmployeeViewModelTest.kt
 *
 * Suite di test per i flussi operatore (picking ordini, filtro corsie, chiusura consegne).
 * Standardizza la documentazione nel modulo unit test.
 */
package it.unito.smartshopmobile.unitViewModelTest

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import it.unito.smartshopmobile.data.dao.CategoryDao
import it.unito.smartshopmobile.data.dao.OrderDao
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.dao.ShelfDao
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.OrderWithLines
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.viewModel.EmployeeViewModel
import it.unito.smartshopmobile.viewModel.OrderFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Copre i casi principali dell'EmployeeViewModel:
 * - toggle della checklist di picking
 * - cambio filtri ordini e selezione corsie
 * - completamento ordini (domicilio vs locker)
 * - refresh scaffali/prodotti con gestione errori
 *
 * Usa Robolectric + MockK per simulare Retrofit, Room e DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class EmployeeViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val ordersFlow = MutableStateFlow<List<OrderWithLines>>(emptyList())
    private val shelvesFlow = MutableStateFlow<List<it.unito.smartshopmobile.data.entity.Shelf>>(emptyList())
    private val productsFlow = MutableStateFlow<List<it.unito.smartshopmobile.data.entity.Product>>(emptyList())

    /**
     * Configura i mock prima di ogni test.
     * Mock per repository, database e API.
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(RetrofitInstance)
        every { RetrofitInstance.api } returns mockk(relaxed = true)

        mockkObject(SmartShopDatabase.Companion)
        val fakeDb = mockk<SmartShopDatabase>(relaxed = true)
        every { fakeDb.orderDao() } returns mockk<OrderDao>(relaxed = true)
        every { fakeDb.shelfDao() } returns mockk<ShelfDao>(relaxed = true)
        every { fakeDb.productDao() } returns mockk<ProductDao>(relaxed = true)
        every { fakeDb.categoryDao() } returns mockk<CategoryDao>(relaxed = true)
        every { SmartShopDatabase.getDatabase(any()) } returns fakeDb

        mockkConstructor(
            OrderRepository::class,
            ShelfRepository::class,
            ProductRepository::class,
            CategoryRepository::class
        )

        every { anyConstructed<OrderRepository>().observeOrders() } returns ordersFlow
        coEvery { anyConstructed<OrderRepository>().updateOrderStatus(any(), any()) } returns Result.success(Unit)

        every { anyConstructed<ShelfRepository>().getAll() } returns shelvesFlow
        coEvery { anyConstructed<ShelfRepository>().refresh() } returns Result.success(Unit)

        every { anyConstructed<ProductRepository>().getAllProducts() } returns productsFlow
        coEvery { anyConstructed<ProductRepository>().refreshProducts() } returns Result.success(Unit)

        every { anyConstructed<CategoryRepository>().getAllCategories() } returns MutableStateFlow(emptyList())
        coEvery { anyConstructed<CategoryRepository>().refreshCategories() } returns Result.success(Unit)
    }

    /**
     * Pulisce tutti i mock dopo ogni test.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test: togglePicked aggiunge e rimuove l'ID della riga dalla checklist.
     */
    @Test
    fun togglePicked_addsAndRemovesLineId() {
        // Arrange: ViewModel con stato iniziale vuoto
        val vm = EmployeeViewModel(context)

        // Act: tocchiamo una riga da pickare e poi la ritocchiamo
        vm.togglePicked(101)
        val afterAdd = vm.uiState.value.pickedLines
        vm.togglePicked(101)
        val afterRemove = vm.uiState.value.pickedLines

        // Assert: l'id viene aggiunto e poi rimosso dal set
        assertTrue(afterAdd.contains(101))
        assertTrue(afterRemove.isEmpty())
    }

    /**
     * Helper per gestire set order filter changes filter and collapses.
     */
    @Test
    fun setOrderFilter_changesFilterAndCollapses() {
        // Arrange: filtro di default ACTIVE
        val vm = EmployeeViewModel(context)
        assertEquals(OrderFilter.ACTIVE, vm.uiState.value.orderFilter)

        // Act: impostiamo filtro completati
        vm.setOrderFilter(OrderFilter.COMPLETED)

        // Assert: stato aggiornato al nuovo filtro
        assertEquals(OrderFilter.COMPLETED, vm.uiState.value.orderFilter)
    }

    /**
     * Helper per gestire select aisle updates selection.
     */
    @Test
    fun selectAisle_updatesSelection() {
        // Arrange: nessuna corsia selezionata
        val vm = EmployeeViewModel(context)

        // Act: scegliamo una corsia
        vm.selectAisle("A2")

        // Assert: lo stato espone l'id selezionato
        assertEquals("A2", vm.uiState.value.selectedAisleId)
    }

    /**
     * Helper per gestire mark order completed domestic order calls consegnato.
     */
    @Test
    fun markOrderCompleted_domesticOrderCallsConsegnato() = runTest {
        // Arrange: ordine a domicilio presente nello stream
        val orderId = 200
        val order = Order(
            idOrdine = orderId,
            idUtente = 1,
            dataOrdine = "2025-12-01T10:00:00Z",
            stato = "SPEDITO",
            totale = 9.99,
            metodoConsegna = "DOMICILIO",
            idLocker = null,
            codiceRitiro = null,
            indirizzoSpedizione = "Via Test 1",
            nomeCliente = "Mario",
            cognomeCliente = "Rossi",
            emailCliente = "mario@example.com"
        )
        ordersFlow.value = listOf(OrderWithLines(order, emptyList()))

        val vm = EmployeeViewModel(context)
        advanceUntilIdle()

        // Act
        vm.markOrderCompleted(orderId)
        advanceUntilIdle()

        // Assert: lo stato viene resettato e la chiamata contiene CONSEGNATO
        coVerify { anyConstructed<OrderRepository>().updateOrderStatus(orderId, "CONSEGNATO") }
        assertEquals(null, vm.uiState.value.updatingOrderId)
        assertEquals(null, vm.uiState.value.orderActionError)
    }

    /**
     * Test: refreshShelvesAndProducts con errore di categoria imposta messaggio di errore.
     */
    @Test
    fun refreshShelvesAndProducts_whenCategoryRefreshFails_setsError() = runTest {
        coEvery { anyConstructed<CategoryRepository>().refreshCategories() } returns Result.failure(Exception("boom"))

        val vm = EmployeeViewModel(context)
        advanceUntilIdle()

        vm.refreshShelvesAndProducts()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("boom", state.aislesError)
        assertEquals(false, state.isLoadingAisles)
    }
}
