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
                .onStart { _uiState.update { it.copy(isLoading = true, errorMessage = null) } }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Errore nel recupero del catalogo"
                        )
                    }
                }
                .collect { products ->
                    _uiState.update { state ->
                        val updated = state.copy(
                            products = products,
                            isLoading = false,
                            errorMessage = null
                        )
                        updated.copy(visibleProducts = filterProducts(updated))
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated.copy(visibleProducts = filterProducts(updated))
        }
    }

    fun onCategorySelected(category: ProductCategory?) {
        _uiState.update { state ->
            val updated = state.copy(selectedCategory = category)
            updated.copy(visibleProducts = filterProducts(updated))
        }
    }

    fun onOnlyOffersToggle() {
        _uiState.update { state ->
            val updated = state.copy(onlyOffers = !state.onlyOffers)
            updated.copy(visibleProducts = filterProducts(updated))
        }
    }

    fun onAvailabilityFilterChange(filter: AvailabilityFilter) {
        _uiState.update { state ->
            val updated = state.copy(availabilityFilter = filter)
            updated.copy(visibleProducts = filterProducts(updated))
        }
    }

    fun onBookmark(productId: String) {
        _uiState.update { state ->
            val updatedProducts = state.products.map { product ->
                if (product.id == productId) product.copy(isFavorite = !product.isFavorite) else product
            }
            val updated = state.copy(products = updatedProducts)
            updated.copy(visibleProducts = filterProducts(updated))
        }
    }

    fun onAddToCart(productId: String) {
        _uiState.update { state ->
            val updatedProducts = state.products.map { product ->
                if (product.id == productId) product.copy(isInCart = !product.isInCart) else product
            }
            val updated = state.copy(products = updatedProducts)
            updated.copy(visibleProducts = filterProducts(updated))
        }
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
    }
}

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val visibleProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val onlyOffers: Boolean = false,
    val availabilityFilter: AvailabilityFilter = AvailabilityFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

enum class AvailabilityFilter(val label: String) {
    ALL("Tutto"),
    ONLY_AVAILABLE("Solo disponibili"),
    INCLUDING_LOW_STOCK("Disponibili + in esaurimento")
}
