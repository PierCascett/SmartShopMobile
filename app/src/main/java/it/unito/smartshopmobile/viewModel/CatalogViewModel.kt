/**
 * CatalogViewModel.kt
 *
 * RUOLO MVVM: ViewModel Layer
 * - Gestisce lo stato UI e la logica di presentazione del catalogo prodotti
 * - Intermedia tra UI (CatalogScreen) e Repository (CatalogRepository)
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
 * - Usa solo il Repository per i dati
 * - Espone UI State immutabile (CatalogUiState)
 *
 * PRINCIPI RISPETTATI:
 * - Single Source of Truth: uiState contiene tutto lo stato
 * - Unidirectional Data Flow: UI → intent → ViewModel → State → UI
 * - Separation of Concerns: logica UI qui, logica dati nel Repository
 */
package it.unito.smartshopmobile.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.model.Product
import it.unito.smartshopmobile.data.model.ProductAvailability
import it.unito.smartshopmobile.data.model.ProductCategory
import it.unito.smartshopmobile.data.repository.CatalogRepository
import it.unito.smartshopmobile.data.repository.FakeCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel MVVM del catalogo: espone solo stato e intent della UI.
// Quando sara' disponibile Retrofit+Room, sostituire FakeCatalogRepository con un'implementazione
// reale iniettata (Hilt/Koin) che interroga il DAO Room e sincronizza via API.
class CatalogViewModel(
    private val repository: CatalogRepository = FakeCatalogRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    init {
        observeCatalog()
    }

    private fun observeCatalog() {
        viewModelScope.launch {
            repository.observeCatalog()
                .onStart { mutateState { it.copy(isLoading = true, errorMessage = null) } }
                .catch { throwable ->
                    mutateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Errore nel recupero del catalogo"
                        )
                    }
                }
                .collect { products ->
                    mutateState {
                        it.copy(
                            products = products,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) = mutateState {
        it.copy(searchQuery = query)
    }

    fun onCategorySelected(category: ProductCategory?) = mutateState {
        it.copy(selectedCategory = category)
    }

    fun onOnlyOffersToggle() = mutateState { current ->
        current.copy(onlyOffers = !current.onlyOffers)
    }

    fun onAvailabilityFilterChange(filter: AvailabilityFilter) = mutateState {
        it.copy(availabilityFilter = filter)
    }

    fun onTagToggle(tag: String) = mutateState { state ->
        val updated = state.selectedTags.toMutableSet().apply {
            if (contains(tag)) remove(tag) else add(tag)
        }
        state.copy(selectedTags = updated)
    }

    fun onBookmark(productId: String) = mutateState { state ->
        val updatedProducts = state.products.map { product ->
            if (product.id == productId) product.copy(isFavorite = !product.isFavorite) else product
        }
        state.copy(products = updatedProducts)
    }

    fun onAddToCart(productId: String) = mutateState { state ->
        val updatedCart = state.cart.toMutableMap().apply {
            val nextQuantity = getOrDefault(productId, 0) + 1
            put(productId, nextQuantity)
        }
        state.copy(cart = updatedCart.toMap())
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

    private fun filterProducts(state: CatalogUiState): List<Product> {
        return state.products
            .filter { product ->
                state.selectedCategory?.let { product.category == it } ?: true
            }
            .filter { product ->
                if (state.onlyOffers) product.oldPrice != null else true
            }
            .filter { product ->
                when (state.availabilityFilter) {
                    AvailabilityFilter.ALL -> true
                    AvailabilityFilter.ONLY_AVAILABLE -> product.availability == ProductAvailability.AVAILABLE
                    AvailabilityFilter.INCLUDING_LOW_STOCK ->
                        product.availability == ProductAvailability.AVAILABLE ||
                            product.availability == ProductAvailability.RUNNING_LOW
                }
            }
            .filter { product ->
                val query = state.searchQuery.trim()
                if (query.isBlank()) true else {
                    product.name.contains(query, ignoreCase = true) ||
                        product.brand.contains(query, ignoreCase = true)
                }
            }
            .filter { product ->
                // tag filter: if none selected, pass all; otherwise product must have at least one selected tag
                state.selectedTags.isEmpty() || product.tags.any { it in state.selectedTags }
            }
    }

    private fun mutateState(transform: (CatalogUiState) -> CatalogUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            recomputeDerivedState(updated)
        }
    }

    private fun recomputeDerivedState(state: CatalogUiState): CatalogUiState {
        val productsWithCartFlag = state.products.map { product ->
            product.copy(isInCart = state.cart.containsKey(product.id))
        }
        val stateWithProducts = state.copy(products = productsWithCartFlag)
        val visibleProducts = filterProducts(stateWithProducts)
        val cartItems = state.cart.mapNotNull { (productId, quantity) ->
            productsWithCartFlag.firstOrNull { it.id == productId }?.let { product ->
                CartItemUi(product = product, quantity = quantity)
            }
        }
        val total = cartItems.sumOf { it.product.price * it.quantity }
        val count = cartItems.sumOf { it.quantity }
        return stateWithProducts.copy(
            visibleProducts = visibleProducts,
            cartItems = cartItems,
            cartItemsCount = count,
            cartTotal = total
        )
    }
}

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val onlyOffers: Boolean = false,
    val selectedTags: Set<String> = emptySet(),
    val availabilityFilter: AvailabilityFilter = AvailabilityFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
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
