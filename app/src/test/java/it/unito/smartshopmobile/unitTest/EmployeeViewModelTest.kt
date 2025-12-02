package it.unito.smartshopmobile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(RetrofitInstance)
        every { RetrofitInstance.api } returns mockk(relaxed = true)

        mockkObject(SmartShopDatabase.Companion)
        val fakeDb = mockk<SmartShopDatabase>(relaxed = true)
        every { fakeDb.orderDao() } returns mockk<OrderDao>(relaxed = true)
        every { fakeDb.productDao() } returns mockk<ProductDao>(relaxed = true)
        every { fakeDb.categoryDao() } returns mockk<CategoryDao>(relaxed = true)
        every { fakeDb.shelfDao() } returns mockk<ShelfDao>(relaxed = true)
        every { SmartShopDatabase.getDatabase(any()) } returns fakeDb

        mockkConstructor(OrderRepository::class, ProductRepository::class, ShelfRepository::class, CategoryRepository::class)

        every { anyConstructed<OrderRepository>().observeOrders() } returns ordersFlow
        coEvery { anyConstructed<OrderRepository>().refreshOrders() } returns Result.success(Unit)
        coEvery { anyConstructed<OrderRepository>().updateOrderStatus(any(), any()) } returns Result.success(Unit)

        every { anyConstructed<ShelfRepository>().getAll() } returns shelvesFlow
        coEvery { anyConstructed<ShelfRepository>().refresh() } returns Result.success(Unit)

        every { anyConstructed<ProductRepository>().getAllProducts() } returns productsFlow
        coEvery { anyConstructed<ProductRepository>().refreshProducts() } returns Result.success(Unit)

        coEvery { anyConstructed<CategoryRepository>().refreshCategories() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

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

    @Test
    fun selectAisle_updatesSelection() {
        // Arrange: nessuna corsia selezionata
        val vm = EmployeeViewModel(context)

        // Act: scegliamo una corsia
        vm.selectAisle("A2")

        // Assert: lo stato espone l'id selezionato
        assertEquals("A2", vm.uiState.value.selectedAisleId)
    }
}
