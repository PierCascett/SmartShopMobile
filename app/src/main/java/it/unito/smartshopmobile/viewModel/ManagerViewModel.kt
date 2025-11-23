/**
 * ManagerViewModel.kt
 *
 * RUOLO MVVM: ViewModel Layer
 * - Gestisce lo stato UI e la logica della schermata Manager
 * - Coordina operazioni amministrative e di supervisione
 * - Intermedia tra UI (ManagerScreen) e Repository (futuro AnalyticsRepository)
 *
 * RESPONSABILITÀ (future):
 * - Visualizzare statistiche vendite
 * - Gestire inventario prodotti
 * - Monitorare performance dipendenti
 * - Generare report
 * - Gestire configurazioni supermercato
 *
 * PATTERN: MVVM (Model-View-ViewModel)
 * - Stato osservabile per dashboard amministrativa
 * - Intent utente (updateInventory, generateReport)
 * - Usa AnalyticsRepository e InventoryRepository
 *
 * ESEMPIO (futuro):
 * class ManagerViewModel(
 *     private val analyticsRepository: AnalyticsRepository,
 *     private val inventoryRepository: InventoryRepository
 * ) : ViewModel() {
 *     val salesStats = analyticsRepository.observeDailySales()
 *         .stateIn(viewModelScope, SharingStarted.Lazily, null)
 * }
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.CreateRestockRequest
import it.unito.smartshopmobile.data.repository.RestockRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.SupplierRepository
import it.unito.smartshopmobile.data.repository.InventoryRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.Supplier
import it.unito.smartshopmobile.data.entity.Shelf
import it.unito.smartshopmobile.data.entity.StockTransferRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ManagerUiState(
    val restocks: List<Restock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val quantity: String = "",
    val successMessage: String? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val availableProducts: List<Product> = emptyList(),
    val selectedProductId: String? = null,
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: Int? = null,
    val showProductDetail: Product? = null,
    val shelves: List<Shelf> = emptyList(),
    val selectedShelfId: Int? = null,
    val transferQuantity: String = "",
    val transferError: String? = null,
    val transferSuccess: String? = null,
    val isTransferring: Boolean = false
)

class ManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SmartShopDatabase.getDatabase(application)
    private val repository = RestockRepository(
        RetrofitInstance.api,
        database.restockDao()
    )
    private val productRepository = ProductRepository(
        database.productDao(),
        RetrofitInstance.api
    )
    private val categoryRepository = CategoryRepository(
        database.categoryDao(),
        RetrofitInstance.api
    )
    private val supplierRepository = SupplierRepository(
        RetrofitInstance.api,
        database.supplierDao()
    )
    private val shelfRepository = ShelfRepository(
        database.shelfDao(),
        RetrofitInstance.api
    )
    private val inventoryRepository = InventoryRepository(
        RetrofitInstance.api
    )
    private val etaFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private var cachedProducts: List<Product> = emptyList()
    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState: StateFlow<ManagerUiState> = _uiState.asStateFlow()

    init {
        observeRestocks()
        observeProductsAndCategories()
        observeSuppliers()
        observeShelves()
        refreshRestocks()
        refreshCatalogData()
    }

    private fun observeRestocks() {
        viewModelScope.launch {
            repository.observeRestocks().collect { list ->
                _uiState.update { it.copy(restocks = list) }
            }
        }
    }

    private fun observeProductsAndCategories() {
        viewModelScope.launch {
            combine(
                categoryRepository.getAllCategories(),
                productRepository.getAllProducts()
            ) { categories, products ->
                categories to products
            }.collect { (categories, products) ->
                cachedProducts = products
                _uiState.update { state ->
                    val selectedCategory = state.selectedCategoryId ?: categories.firstOrNull()?.id
                    val filtered = filterProductsByCategory(selectedCategory)
                    val selectedProduct = state.selectedProductId?.takeIf { id ->
                        filtered.any { it.id == id }
                    } ?: filtered.firstOrNull()?.id
                    state.copy(
                        categories = categories,
                        selectedCategoryId = selectedCategory,
                        availableProducts = filtered,
                        selectedProductId = selectedProduct
                    )
                }
            }
        }
    }

    private fun observeSuppliers() {
        viewModelScope.launch {
            supplierRepository.observeSuppliers().collect { suppliers ->
                _uiState.update { state ->
                    val selectedSupplier = state.selectedSupplierId?.takeIf { id ->
                        suppliers.any { it.id == id }
                    } ?: suppliers.firstOrNull()?.id
                    state.copy(suppliers = suppliers, selectedSupplierId = selectedSupplier)
                }
            }
        }
    }

    private fun observeShelves() {
        viewModelScope.launch {
            shelfRepository.getAll().collect { shelves ->
                _uiState.update { state ->
                    val selected = state.selectedShelfId?.takeIf { id ->
                        shelves.any { it.id == id }
                    } ?: shelves.firstOrNull()?.id
                    state.copy(shelves = shelves, selectedShelfId = selected)
                }
            }
        }
    }

    private fun filterProductsByCategory(categoryId: String?): List<Product> {
        val filtered = if (categoryId.isNullOrBlank()) {
            cachedProducts
        } else {
            cachedProducts.filter { it.categoryId == categoryId }
        }
        return mergeProductVariants(filtered)
    }

    private fun mergeProductVariants(products: List<Product>): List<Product> {
        return products
            .groupBy { it.id }
            .map { (_, variants) ->
                val first = variants.first()
                if (variants.size == 1) {
                    first
                } else {
                    first.copy(
                        catalogQuantity = variants.sumOf { it.catalogQuantity },
                        warehouseQuantity = variants.sumOf { it.warehouseQuantity },
                        totalQuantity = variants.sumOf { it.totalQuantity }
                    )
                }
            }
            .sortedBy { it.name }
    }

    fun refreshRestocks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.fetchRestocks()
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    private fun refreshCatalogData() {
        viewModelScope.launch {
            categoryRepository.refreshCategories()
            productRepository.refreshProducts()
            supplierRepository.refreshSuppliers()
            shelfRepository.refresh()
        }
    }

    fun submitRestock(responsabileId: Int? = null) {
        val state = _uiState.value
        val qty = state.quantity.toIntOrNull()
        val productId = state.selectedProductId
        val supplierId = state.selectedSupplierId
        if (productId.isNullOrBlank() || supplierId == null || qty == null || qty <= 0) {
            _uiState.update { it.copy(error = "Seleziona prodotto, fornitore e quantita valide") }
            return
        }
        val eta = Instant.now().plusSeconds(30).atZone(ZoneId.systemDefault()).format(etaFormatter)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val request = CreateRestockRequest(
                idProdotto = productId,
                idFornitore = supplierId,
                quantitaOrdinata = qty,
                dataArrivoPrevista = eta,
                idResponsabile = responsabileId
            )
            repository.createRestock(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Riordino creato per $productId",
                            quantity = ""
                        )
                    }
                    refreshRestocks()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun moveStockToShelf() {
        val state = _uiState.value
        val qty = state.transferQuantity.toIntOrNull()
        val productId = state.selectedProductId
        val shelfId = state.selectedShelfId

        if (productId.isNullOrBlank() || shelfId == null || qty == null || qty <= 0) {
            _uiState.update { it.copy(transferError = "Seleziona prodotto, scaffale e quantita' valide") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTransferring = true,
                    transferError = null,
                    transferSuccess = null
                )
            }
            val request = StockTransferRequest(
                idProdotto = productId,
                quantita = qty,
                idScaffale = shelfId
            )
            inventoryRepository.moveStock(request)
                .onSuccess {
                    productRepository.refreshProducts()
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            transferSuccess = "Spostati $qty pezzi su scaffale $shelfId",
                            transferError = null,
                            transferQuantity = ""
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            transferError = error.message
                        )
                    }
                }
        }
    }

    fun onQuantityChanged(value: String) {
        _uiState.update { it.copy(quantity = value.filter { ch -> ch.isDigit() }) }
    }

    fun onTransferQuantityChanged(value: String) {
        _uiState.update { it.copy(transferQuantity = value.filter { ch -> ch.isDigit() }) }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { state ->
            val filtered = filterProductsByCategory(categoryId)
            state.copy(
                selectedCategoryId = categoryId,
                availableProducts = filtered,
                selectedProductId = filtered.firstOrNull()?.id
            )
        }
    }

    fun onProductSelected(productId: String?) {
        _uiState.update { it.copy(selectedProductId = productId) }
    }

    fun onSupplierSelected(supplierId: Int?) {
        _uiState.update { it.copy(selectedSupplierId = supplierId) }
    }

    fun onShelfSelected(shelfId: Int?) {
        _uiState.update { it.copy(selectedShelfId = shelfId) }
    }

    fun showProductDetail(productId: String) {
        val product = cachedProducts.firstOrNull { it.id == productId }
        _uiState.update { it.copy(showProductDetail = product) }
    }

    fun dismissProductDetail() {
        _uiState.update { it.copy(showProductDetail = null) }
    }
} 

