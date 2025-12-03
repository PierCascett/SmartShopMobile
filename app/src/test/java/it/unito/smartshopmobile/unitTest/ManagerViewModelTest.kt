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
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.dao.RestockDao
import it.unito.smartshopmobile.data.dao.ShelfDao
import it.unito.smartshopmobile.data.dao.SupplierDao
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.Shelf
import it.unito.smartshopmobile.data.entity.StockTransferResult
import it.unito.smartshopmobile.data.entity.Supplier
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.InventoryRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.RestockRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.repository.SupplierRepository
import it.unito.smartshopmobile.viewModel.ManagerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ManagerViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val restockFlow = MutableStateFlow<List<Restock>>(emptyList())
    private val categoryFlow = MutableStateFlow<List<Category>>(emptyList())
    private val productFlow = MutableStateFlow<List<Product>>(emptyList())
    private val supplierFlow = MutableStateFlow<List<Supplier>>(emptyList())
    private val shelfFlow = MutableStateFlow<List<Shelf>>(emptyList())

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(RetrofitInstance)
        every { RetrofitInstance.api } returns mockk(relaxed = true)

        mockkObject(SmartShopDatabase.Companion)
        val fakeDb = mockk<SmartShopDatabase>(relaxed = true)
        every { fakeDb.restockDao() } returns mockk<RestockDao>(relaxed = true)
        every { fakeDb.productDao() } returns mockk<ProductDao>(relaxed = true)
        every { fakeDb.categoryDao() } returns mockk<CategoryDao>(relaxed = true)
        every { fakeDb.supplierDao() } returns mockk<SupplierDao>(relaxed = true)
        every { fakeDb.shelfDao() } returns mockk<ShelfDao>(relaxed = true)
        every { SmartShopDatabase.getDatabase(any()) } returns fakeDb

        mockkConstructor(
            RestockRepository::class,
            ProductRepository::class,
            CategoryRepository::class,
            SupplierRepository::class,
            ShelfRepository::class,
            InventoryRepository::class
        )

        every { anyConstructed<RestockRepository>().observeRestocks() } returns restockFlow
        coEvery { anyConstructed<RestockRepository>().fetchRestocks() } returns Result.success(Unit)
        coEvery { anyConstructed<RestockRepository>().createRestock(any()) } returns Result.success(mockk(relaxed = true))

        every { anyConstructed<ProductRepository>().getAllProducts() } returns productFlow
        coEvery { anyConstructed<ProductRepository>().refreshProducts() } returns Result.success(Unit)

        every { anyConstructed<CategoryRepository>().getAllCategories() } returns categoryFlow
        coEvery { anyConstructed<CategoryRepository>().refreshCategories() } returns Result.success(Unit)

        every { anyConstructed<SupplierRepository>().observeSuppliers() } returns supplierFlow
        coEvery { anyConstructed<SupplierRepository>().refreshSuppliers() } returns Result.success(Unit)

        every { anyConstructed<ShelfRepository>().getAll() } returns shelfFlow
        coEvery { anyConstructed<ShelfRepository>().refresh() } returns Result.success(Unit)

        coEvery { anyConstructed<InventoryRepository>().reconcileArrivals() } returns Result.success(Unit)
        coEvery { anyConstructed<InventoryRepository>().moveStock(any()) } returns Result.success(
            mockk<StockTransferResult>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onQuantityChanged_keepsOnlyDigits() {
        // Arrange: input con caratteri misti
        val vm = ManagerViewModel(context)

        // Act: aggiorniamo la quantità con testo sporco
        vm.onQuantityChanged("12a3")

        // Assert: rimangono solo le cifre
        assertEquals("123", vm.uiState.value.quantity)
    }

    @Test
    fun onTransferQuantityChanged_keepsOnlyDigits() {
        // Arrange: input con lettere
        val vm = ManagerViewModel(context)

        // Act: aggiorniamo il campo per lo spostamento
        vm.onTransferQuantityChanged("9b8c")

        // Assert: solo numeri restano nel campo
        assertEquals("98", vm.uiState.value.transferQuantity)
    }

    @Test
    fun onCategorySelected_updatesSelection() {
        // Arrange: nessuna categoria selezionata di default
        val vm = ManagerViewModel(context)

        // Act: scegliamo una categoria
        vm.onCategorySelected("cat-1")

        // Assert: selectedCategoryId aggiornato
        assertEquals("cat-1", vm.uiState.value.selectedCategoryId)
    }

    @Test
    fun onProductSelected_updatesSelection() {
        // Arrange: ViewModel pronto
        val vm = ManagerViewModel(context)

        // Act: selezioniamo un prodotto
        vm.onProductSelected("prod-42")

        // Assert: selectedProductId aggiornato
        assertEquals("prod-42", vm.uiState.value.selectedProductId)
    }
}
