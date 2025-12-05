/**
 * ManagerViewModelTest.kt
 *
 * Suite di test unitari per il flusso manager (inventario, riordini, trasferimenti).
 * Aggiunge documentazione di file coerente con il resto del progetto.
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica il comportamento del ManagerViewModel su:
 * - normalizzazione input quantità/trasferimenti
 * - selezione categorie/prodotti
 * - trasferimenti stock magazzino->scaffale e viceversa
 * - creazione riordini e gestione messaggi di stato/errore
 * - refresh completo dei dati con indicatori di loading
 */
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

    /**
     * Configura i mock prima di ogni test.
     *
     * Inizializza tutti i mock necessari per i repository, il database e l'API.
     * Configura comportamenti di default per evitare chiamate reali alla rete o al database.
     */
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

    /**
     * Pulisce tutti i mock dopo ogni test per evitare interferenze.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test: onQuantityChanged mantiene solo le cifre numeriche.
     * Verifica che l'input venga sanitizzato rimuovendo caratteri non numerici.
     */
    @Test
    fun onQuantityChanged_keepsOnlyDigits() {
        // Arrange: input con caratteri misti
        val vm = ManagerViewModel(context)

        // Act: aggiorniamo la quantità con testo sporco
        vm.onQuantityChanged("12a3")

        // Assert: rimangono solo le cifre
        assertEquals("123", vm.uiState.value.quantity)
    }

    /**
     * Test: onTransferQuantityChanged mantiene solo le cifre numeriche.
     * Verifica che l'input per il trasferimento venga sanitizzato.
     */
    @Test
    fun onTransferQuantityChanged_keepsOnlyDigits() {
        // Arrange: input con lettere
        val vm = ManagerViewModel(context)

        // Act: aggiorniamo il campo per lo spostamento
        vm.onTransferQuantityChanged("9b8c")

        // Assert: solo numeri restano nel campo
        assertEquals("98", vm.uiState.value.transferQuantity)
    }

    /**
     * Helper per gestire on category selected updates selection.
     */
    @Test
    fun onCategorySelected_updatesSelection() {
        // Arrange: nessuna categoria selezionata di default
        val vm = ManagerViewModel(context)

        // Act: scegliamo una categoria
        vm.onCategorySelected("cat-1")

        // Assert: selectedCategoryId aggiornato
        assertEquals("cat-1", vm.uiState.value.selectedCategoryId)
    }

    /**
     * Helper per gestire on product selected updates selection.
     */
    @Test
    fun onProductSelected_updatesSelection() {
        // Arrange: ViewModel pronto
        val vm = ManagerViewModel(context)

        // Act: selezioniamo un prodotto
        vm.onProductSelected("prod-42")

        // Assert: selectedProductId aggiornato
        assertEquals("prod-42", vm.uiState.value.selectedProductId)
    }

    /**
     * Helper per gestire move stock to shelf when warehouse empty sets error message.
     */
    @Test
    fun moveStockToShelf_whenWarehouseEmpty_setsErrorMessage() = runTest {
        // Arrange: prodotto senza scorte di magazzino e scaffale disponibile
        val product = Product(
            catalogId = 1,
            id = "prd-01",
            name = "Mela",
            brand = "BrandX",
            categoryId = "cat-1",
            categoryName = "Frutta",
            categoryDescription = null,
            catalogQuantity = 1,
            warehouseQuantity = 0,
            totalQuantity = 1,
            price = 2.5,
            oldPrice = null,
            availability = "OK",
            tags = emptyList(),
            description = null,
            imageUrl = null,
            shelfId = 1
        )
        productFlow.value = listOf(product)
        shelfFlow.value = listOf(Shelf(id = 1, nome = "S1", descrizione = "desc"))

        val vm = ManagerViewModel(context)
        advanceUntilIdle()

        vm.onProductSelected(product.id)
        vm.onTransferQuantityChanged("2")

        // Act
        vm.moveStockToShelf()
        advanceUntilIdle()

        // Assert: errore per magazzino vuoto
        assertEquals("Magazzino vuoto per questo prodotto", vm.uiState.value.transferError)
    }

    /**
     * Helper per gestire submit restock success sets success message and resets quantity.
     */
    @Test
    fun submitRestock_success_setsSuccessMessageAndResetsQuantity() = runTest {
        // Arrange: prodotto, fornitore e quantità validi
        val category = Category(id = "cat-1", nome = "Frutta")
        categoryFlow.value = listOf(category)

        val product = Product(
            catalogId = 4,
            id = "prd-04",
            name = "Detersivo",
            brand = "CasaPulita",
            categoryId = category.id,
            categoryName = category.nome,
            categoryDescription = null,
            catalogQuantity = 5,
            warehouseQuantity = 10,
            totalQuantity = 15,
            price = 1.99,
            oldPrice = null,
            availability = "OK",
            tags = emptyList(),
            description = null,
            imageUrl = null,
            shelfId = 1
        )
        productFlow.value = listOf(product)
        val supplier = Supplier(id = 10, name = "FornitoreX", phone = null, email = null, address = null)
        supplierFlow.value = listOf(supplier)
        coEvery { anyConstructed<RestockRepository>().createRestock(any()) } returns Result.success(
            Restock(
                idRiordino = 1,
                idProdotto = product.id,
                prodottoNome = product.name,
                idFornitore = supplier.id,
                fornitoreNome = supplier.name,
                quantitaOrdinata = 3,
                dataOrdine = "2025-12-01T12:00:00Z",
                dataArrivoPrevista = null,
                dataArrivoEffettiva = null,
                arrivato = false,
                idResponsabile = null,
                responsabileNome = null,
                responsabileCognome = null
            )
        )

        val vm = ManagerViewModel(context)
        advanceUntilIdle()

        vm.onProductSelected(product.id)
        vm.onSupplierSelected(supplier.id)
        vm.onQuantityChanged("3")

        // Act
        vm.submitRestock(responsabileId = 5)
        advanceUntilIdle()
        advanceTimeBy(36_000)

        // Assert: messaggio di successo e quantità azzerata
        val state = vm.uiState.value
        assertEquals("Riordino creato per ${product.id}", state.successMessage)
        assertEquals("", state.quantity)
        coVerify { anyConstructed<RestockRepository>().createRestock(any()) }
    }

    /**
     * Test: refreshAllData aggiorna i flag di loading.
     * Verifica che il refresh completisolo stato di loading correttamente.
     */
    @Test
    fun refreshAllData_setsLoadingFlags() = runTest {
        val vm = ManagerViewModel(context)
        advanceUntilIdle()

        vm.refreshAllData()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isLoading)
    }
}
