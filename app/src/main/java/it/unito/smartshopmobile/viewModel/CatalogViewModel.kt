/**
 * CatalogViewModel.kt
 *
 * RUOLO MVVM: ViewModel Layer
 * - Gestisce lo stato UI e la logica di presentazione del catalogo prodotti
 * - Intermedia tra UI (CatalogScreen) e Repository (ProdottoRepository, CategoriaRepository)
 * - Sopravvive ai cambiamenti di configurazione (rotazione schermo)
 *
 * RESPONSABILITÀ:
 * - Esporre stato UI osservabile (StateFlow<CatalogUiState>)
 * - Gestire intent utente (ricerca, filtri, aggiungi al carrello)
 * - Coordinare operazioni asincrone (viewModelScope)
 * - Trasformare dati dal Repository in stato UI
 *
 * PATTERN: MVVM (Model-View-ViewModel)
 * - NON conosce Compose, Activity o Fragment
 * - NON accede direttamente a Room o Retrofit
 * - Usa solo i Repository per i dati
 * - Espone UI State immutabile (CatalogUiState)
 *
 * PRINCIPI RISPETTATI:
 * - Single Source of Truth: uiState contiene tutto lo stato
 * - Unidirectional Data Flow: UI → intent → ViewModel → State → UI
 * - Separation of Concerns: logica UI qui, logica dati nel Repository
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.entity.CreateOrderRequest
import it.unito.smartshopmobile.data.entity.OrderItemRequest
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.ui.screens.SideMenuSection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val database = SmartShopDatabase.getDatabase(application)
    private val productRepository = ProductRepository(
        database.productDao(),
        RetrofitInstance.api
    )
    private val categoryRepository = CategoryRepository(
        database.categoryDao(),
        RetrofitInstance.api
    )
    private val shelfRepository = ShelfRepository(
        database.shelfDao(),
        RetrofitInstance.api
    )
    private val orderRepository = OrderRepository(
        RetrofitInstance.api,
        database.orderDao()
    )

    // UI State
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    private var started = false

    init {
        observeData()
    }

    /** Avvia la sincronizzazione da rete verso Room (una sola volta dopo login customer) */
    fun startSyncIfNeeded() {
        if (started) return
        started = true
        refreshData()
    }

    private fun observeData() {
        // Osserva i prodotti dal database locale
        viewModelScope.launch {
            combine(
                productRepository.getAllProducts(),
                categoryRepository.getAllCategories(),
                shelfRepository.getAll()
            ) { products, categories, shelves ->
                Triple(products, categories, shelves)
            }
                .onStart { mutateState { it.copy(isLoading = true, errorMessage = null) } }
                .catch { throwable ->
                    mutateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Errore nel recupero del catalogo"
                        )
                    }
                }
                .collect { (products, categories, shelves) ->
                    val mergedProducts = mergeProductsById(products)
                    mutateState {
                        it.copy(
                            allProducts = mergedProducts,
                            allCategories = categories,
                            shelves = shelves,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun mergeProductsById(products: List<Product>): List<Product> {
        return products
            .groupBy { it.id }
            .map { (_, group) ->
                val first = group.first()
                val sumCatalog = group.sumOf { it.catalogQuantity }
                val sumWarehouse = group.sumOf { it.warehouseQuantity }
                val sumTotal = group.sumOf { it.totalQuantity }
                first.copy(
                    catalogQuantity = sumCatalog,
                    warehouseQuantity = sumWarehouse,
                    totalQuantity = sumTotal
                )
            }
    }

    fun setLoggedUser(user: User) = mutateState { it.copy(loggedUser = user) }

    fun clearSession() {
        started = false
        mutateState { CatalogUiState() }
    }

    private fun refreshData() {
        viewModelScope.launch {
            // Sincronizza categorie
            categoryRepository.refreshCategories()
                .onFailure { error ->
                    mutateState { it.copy(errorMessage = "Errore sincronizzazione categorie: ${error.message}") }
                }

            // Sincronizza prodotti
            productRepository.refreshProducts()
                .onFailure { error ->
                    mutateState { it.copy(errorMessage = "Errore sincronizzazione prodotti: ${error.message}") }
                }

            // Sincronizza scaffali
            shelfRepository.refresh()
                .onFailure { error ->
                    mutateState { it.copy(errorMessage = "Errore sincronizzazione scaffali: ${error.message}") }
                }
        }
    }

    fun retry() {
        refreshData()
    }

    fun refreshCatalog() {
        refreshData()
    }

    fun onSearchQueryChange(query: String) = mutateState {
        it.copy(searchQuery = query)
    }

    fun onCategorySelected(categoryId: String?) = mutateState {
        it.copy(selectedCategoryId = categoryId)
    }

    fun onOnlyOffersToggle() = mutateState { current ->
        current.copy(onlyOffers = !current.onlyOffers)
    }

    fun onAvailabilityFilterChange(filter: AvailabilityFilter) = mutateState {
        it // availability filter disabilitato
    }

    fun onTagToggle(tag: String) = mutateState { state ->
        val updated = state.selectedTags.toMutableSet().apply {
            if (contains(tag)) remove(tag) else add(tag)
        }
        state.copy(selectedTags = updated)
    }

    fun onBookmark(productId: String) {
        // TODO: Implementare preferiti quando avremo la tabella nel DB
    }

    fun onAddToCart(productId: String) = mutateState { state ->
        val product = state.allProducts.firstOrNull { it.id == productId }
            ?: return@mutateState state
        val current = state.cart[productId] ?: 0
        if (current >= product.catalogQuantity) {
            return@mutateState state.copy(
                toastMessage = "Disponibili solo ${product.catalogQuantity} pezzi di ${product.name}",
                showToast = true
            )
        }
        val updatedCart = state.cart.toMutableMap().apply {
            put(productId, current + 1)
        }
        state.copy(cart = updatedCart.toMap(), showToast = false, toastMessage = null)
    }

    fun onDecreaseCartItem(productId: String) = mutateState { state ->
        val currentQuantity = state.cart[productId] ?: return@mutateState state
        val updatedCart = state.cart.toMutableMap().apply {
            if (currentQuantity <= 1) remove(productId) else put(productId, currentQuantity - 1)
        }
        state.copy(cart = updatedCart.toMap())
    }

    fun onRemoveFromCart(productId: String) = mutateState { state ->
        if (!state.cart.containsKey(productId)) return@mutateState state
        val updatedCart = state.cart.toMutableMap().apply { remove(productId) }
        state.copy(cart = updatedCart.toMap())
    }

    fun consumeToast() = mutateState { it.copy(showToast = false, toastMessage = null) }

    private fun filterProducts(state: CatalogUiState): List<Product> {
        return state.allProducts
            .filter { product ->
                state.selectedCategoryId?.let { product.categoryId == it } ?: true
            }
            .filter { product ->
                if (state.onlyOffers) product.oldPrice != null else true
            }
            .filter { product ->
                val query = state.searchQuery.trim()
                if (query.isBlank()) true else {
                    product.name.contains(query, ignoreCase = true) ||
                        product.brand.contains(query, ignoreCase = true)
                }
            }
            .filter { product ->
                if (state.selectedTags.isEmpty()) {
                    true
                } else {
                    val tags = parseTagsFromJson(product.tags)
                    tags.any { it in state.selectedTags }
                }
            }
    }

    private fun parseTagsFromJson(tags: List<String>?): List<String> = tags ?: emptyList()

    private fun mutateState(transform: (CatalogUiState) -> CatalogUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            recomputeDerivedState(updated)
        }
    }

    private fun recomputeDerivedState(state: CatalogUiState): CatalogUiState {
        val visibleProducts = filterProducts(state)
        val cartItems = state.cart.mapNotNull { (productId, quantity) ->
            state.allProducts.firstOrNull { it.id == productId }?.let { product ->
                CartItemUi(product = product, quantity = quantity)
            }
        }
        val total = cartItems.sumOf { (it.product.price * it.quantity).toDouble() }
        val count = cartItems.sumOf { it.quantity }

        val menuSections = listOf(
            SideMenuSection(
                id = "categories",
                title = "Categorie",
                entries = state.allCategories
                    .sortedBy { it.nome }
                    .map { it.nome }
            )
        )

        // Estrae tutti i tag unici dai prodotti visibili
        val allAvailableTags = visibleProducts
            .flatMap { parseTagsFromJson(it.tags) }
            .distinct()
            .sorted()

        return state.copy(
            visibleProducts = visibleProducts,
            cartItems = cartItems,
            cartItemsCount = count,
            cartTotal = total,
            sideMenuSections = menuSections,
            allAvailableTags = allAvailableTags
        )
    }

    fun submitOrder() {
        val snapshot = _uiState.value
        val user = snapshot.loggedUser
        if (user == null) {
            mutateState { it.copy(orderError = "Sessione utente non disponibile") }
            return
        }
        if (snapshot.cart.isEmpty()) {
            mutateState { it.copy(orderError = "Il carrello è vuoto") }
            return
        }
        val items = snapshot.cart.map { (productId, qty) ->
            OrderItemRequest(idProdotto = productId, quantita = qty)
        }
        viewModelScope.launch {
            mutateState { it.copy(isSubmittingOrder = true, orderError = null, lastOrderId = null) }
            val result = orderRepository.createOrder(
                CreateOrderRequest(
                    idUtente = user.id,
                    items = items
                )
            )
            result.onSuccess { created ->
                // aggiorna ordini per i dipendenti/local cache
                orderRepository.refreshOrders()
                // aggiorna catalogo e scaffali per riflettere quantità scalate
                refreshData()
                mutateState {
                    recomputeDerivedState(
                        it.copy(
                            cart = emptyMap(),
                            isSubmittingOrder = false,
                            orderError = null,
                            lastOrderId = created.idOrdine
                        )
                    )
                }
            }.onFailure { error ->
                mutateState { it.copy(isSubmittingOrder = false, orderError = error.message) }
            }
        }
    }
}

data class CatalogUiState(
    val allProducts: List<Product> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val shelves: List<it.unito.smartshopmobile.data.entity.Shelf> = emptyList(),
    val loggedUser: User? = null,
    val visibleProducts: List<Product> = emptyList(),
    val sideMenuSections: List<SideMenuSection> = emptyList(), // <-- categorie per menu laterale
    val allAvailableTags: List<String> = emptyList(), // <-- tutti i tag unici disponibili
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val onlyOffers: Boolean = false,
    val selectedTags: Set<String> = emptySet(),
    val availabilityFilter: AvailabilityFilter = AvailabilityFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSubmittingOrder: Boolean = false,
    val orderError: String? = null,
    val lastOrderId: Int? = null,
    val showToast: Boolean = false,
    val toastMessage: String? = null,
    val cart: Map<String, Int> = emptyMap(),
    val cartItems: List<CartItemUi> = emptyList(),
    val cartItemsCount: Int = 0,
    val cartTotal: Double = 0.0
)

enum class AvailabilityFilter(val label: String) {
    ALL("Tutto"),
    ONLY_AVAILABLE("Solo disponibili"),
    INCLUDING_LOW_STOCK("Disponibili + in esaurimento")
}

data class CartItemUi(
    val product: Product,
    val quantity: Int
)















