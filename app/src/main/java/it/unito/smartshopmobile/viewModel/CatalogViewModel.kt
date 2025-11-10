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
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
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

    // UI State
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    init {
        observeData()
        refreshData()
    }

    private fun observeData() {
        // Osserva i prodotti dal database locale
        viewModelScope.launch {
            combine(
                productRepository.getAllProducts(),
                categoryRepository.getAllCategories()
            ) { products, categories ->
                Pair(products, categories)
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
                .collect { (products, categories) ->
                    mutateState {
                        it.copy(
                            allProducts = products,
                            allCategories = categories,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
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
        }
    }

    fun retry() {
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
        it.copy(availabilityFilter = filter)
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
        return state.allProducts
            .filter { product ->
                state.selectedCategoryId?.let { product.categoryId == it } ?: true
            }
            .filter { product ->
                if (state.onlyOffers) product.oldPrice != null else true
            }
            .filter { product ->
                when (state.availabilityFilter) {
                    AvailabilityFilter.ALL -> true
                    AvailabilityFilter.ONLY_AVAILABLE -> product.availability == "Disponibile"
                    AvailabilityFilter.INCLUDING_LOW_STOCK ->
                        product.availability == "Disponibile" ||
                            product.availability == "Quasi esaurito"
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
                if (state.selectedTags.isEmpty()) {
                    true
                } else {
                    val tags = parseTagsFromJson(product.tags)
                    tags.any { it in state.selectedTags }
                }
            }
    }

    private fun parseTagsFromJson(tagJson: String?): List<String> {
        if (tagJson.isNullOrBlank()) return emptyList()
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(tagJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

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

        // Converte le categorie del DB in SideMenuSection
        // Le macro-categorie hanno parent_id = null (o "null" come stringa)
        // Le sottocategorie hanno parent_id = <id_macro>
        val macroCategories = state.allCategories
            .filter { it.parentId == null || it.parentId == "null" }
            .sortedBy { it.ordine }

        // DEBUG: log delle categorie
        android.util.Log.d("CatalogVM", "Total categories: ${state.allCategories.size}")
        android.util.Log.d("CatalogVM", "Macro categories: ${macroCategories.size}")

        // DEBUG: mostra TUTTE le categorie con parent_id
        state.allCategories.forEach { cat ->
            android.util.Log.d("CatalogVM", "Cat: id='${cat.id}', title='${cat.titolo}', gruppo='${cat.gruppo}', parent_id='${cat.parentId}'")
        }

        val menuSections = macroCategories.map { macro ->
            android.util.Log.d("CatalogVM", "=== Cercando sottocategorie per macro.id='${macro.id}' (titolo='${macro.titolo}') ===")
            android.util.Log.d("CatalogVM", "macro.id type: ${macro.id::class.java.name}, length: ${macro.id.length}")

            // Trova tutte le sottocategorie di questa macro-categoria
            val subcategories = state.allCategories
                .filter {
                    // Trim degli ID per evitare problemi con spazi bianchi
                    val parentIdTrimmed = it.parentId?.trim()
                    val macroIdTrimmed = macro.id.trim()
                    val match = parentIdTrimmed == macroIdTrimmed
                    if (it.parentId != null) {
                        android.util.Log.d("CatalogVM", "  Confronto: parentId='${it.parentId}' (trimmed='$parentIdTrimmed', len=${it.parentId.length}) == macro.id='${macro.id}' (trimmed='$macroIdTrimmed', len=${macro.id.length}) ? $match (titolo='${it.titolo}')")
                    }
                    match
                }
                .sortedBy { it.ordine }
                .map { it.titolo }

            android.util.Log.d("CatalogVM", "Macro: ${macro.id} (${macro.titolo}) -> ${subcategories.size} sottocategorie")
            subcategories.forEachIndexed { i, sub ->
                android.util.Log.d("CatalogVM", "  [$i] $sub")
            }

            SideMenuSection(
                id = macro.id,
                title = macro.titolo,  // Es: "Carne e Pesce", "Frutta e Verdura"
                entries = subcategories  // Es: ["Pollo e Tacchino", "Manzo e Vitello", ...]
            )
        }

        // Estrae tutti i tag unici dai prodotti visibili
        val allAvailableTags = visibleProducts
            .mapNotNull { it.tags }
            .flatMap { parseTagsFromJson(it) }
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
}

data class CatalogUiState(
    val allProducts: List<Product> = emptyList(),
    val allCategories: List<Category> = emptyList(),
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















