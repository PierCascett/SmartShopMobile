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
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.data.repository.UserRepository
import it.unito.smartshopmobile.ui.screens.SideMenuEntry
import it.unito.smartshopmobile.ui.screens.SideMenuSection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import it.unito.smartshopmobile.data.entity.UpdateUserRequest
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.datastore.FavoritesDataStore
import kotlin.random.Random

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
    private val userRepository = UserRepository(RetrofitInstance.api)
    private val sessionDataStore = SessionDataStore(application)
    private val favoritesDataStore = FavoritesDataStore(application)

    // UI State
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    private var started = false
    private var favoritesJob: Job? = null
    private val fallbackOrderFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    init {
        observeData()
        observeOrderHistory()
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

    private fun observeOrderHistory() {
        viewModelScope.launch {
            orderRepository.observeOrders().collect { ordersWithLines ->
                val userId = _uiState.value.loggedUser?.id
                if (userId == null) {
                    mutateState { it.copy(orderHistory = emptyList()) }
                } else {
                    val normalized = ordersWithLines
                        .map { it.order.copy(righe = it.lines) }
                        .filter { it.idUtente == userId }
                        .map { order ->
                            if (order.metodoConsegna.equals("LOCKER", true)) {
                                val code = lockerCodeFor(order.idOrdine)
                                order.copy(codiceRitiro = code)
                            } else order
                        }
                    val ordered = normalized.sortedByDescending { orderTimestamp(it.dataOrdine) }
                    val numbered = ordered.mapIndexed { index, order ->
                        CustomerOrderHistoryEntry(order, index + 1)
                    }
                    mutateState { it.copy(orderHistory = numbered) }
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

    fun setLoggedUser(user: User) {
        mutateState { it.copy(loggedUser = user) }
        observeFavorites(user.id)
    }

    fun clearSession() {
        started = false
        favoritesJob?.cancel()
        favoritesJob = null
        mutateState { CatalogUiState() }
    }

    fun setCustomerContacts(indirizzo: String, telefono: String?) {
        mutateState {
            it.copy(
                shippingAddress = indirizzo,
                shippingPhone = telefono.orEmpty()
            )
        }
    }

    suspend fun updateCustomerProfile(
        nome: String,
        cognome: String,
        email: String,
        telefono: String?
    ): Result<User> {
        val current = _uiState.value.loggedUser ?: return Result.failure(Exception("Utente non loggato"))
        val result = userRepository.updateProfile(
            current.id,
            UpdateUserRequest(nome = nome, cognome = cognome, email = email, telefono = telefono)
        )
        result.onSuccess { updated ->
            mutateState { it.copy(loggedUser = updated) }
            sessionDataStore.saveUser(updated)
        }
        return result
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

    fun onParentCategorySelected(parentId: String?) = mutateState {
        it.copy(selectedParentId = parentId, selectedCategoryId = null)
    }

    fun onCategorySelected(categoryId: String?) = mutateState { current ->
        val parentId = categoryId?.let { id ->
            current.allCategories.firstOrNull { cat -> cat.id == id }?.parentId
        }
        current.copy(selectedCategoryId = categoryId, selectedParentId = parentId)
    }

    fun onProductSelected(productId: String?) = mutateState { state ->
        val product = state.allProducts.firstOrNull { it.id == productId }
        state.copy(selectedProduct = product)
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
        val userId = _uiState.value.loggedUser?.id ?: return
        viewModelScope.launch {
            val current = _uiState.value.favoriteProductIds
            val updated = if (current.contains(productId)) current - productId else current + productId
            mutateState { it.copy(favoriteProductIds = updated) }
            favoritesDataStore.saveFavorites(userId, updated)
        }
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

    fun onDeliveryMethodSelected(method: DeliveryMethod) = mutateState { state ->
        state.copy(deliveryMethod = method)
    }

    fun consumeToast() = mutateState { it.copy(showToast = false, toastMessage = null) }

    fun clearOrderFeedback() = mutateState { it.copy(lastOrderId = null) }

    fun clearPickupMessage() = mutateState { it.copy(pickupMessage = null) }

    fun refreshOrderHistory() {
        if (_uiState.value.loggedUser == null) {
            mutateState { it.copy(orderHistoryError = "Accedi per visualizzare lo storico ordini") }
            return
        }
        viewModelScope.launch {
            mutateState { it.copy(isOrderHistoryLoading = true, orderHistoryError = null) }
            val result = orderRepository.refreshOrders()
            result.onSuccess {
                mutateState { it.copy(isOrderHistoryLoading = false) }
            }.onFailure { error ->
                mutateState {
                    it.copy(
                        isOrderHistoryLoading = false,
                        orderHistoryError = error.message ?: "Errore nel recupero dello storico"
                    )
                }
            }
        }
    }

    fun simulateLockerPickup(orderId: Int) {
        val snapshot = _uiState.value
        if (snapshot.pickupInProgressId != null) return
        val order = snapshot.orderHistory.firstOrNull { it.order.idOrdine == orderId }?.order
        if (order == null || !order.metodoConsegna.equals("LOCKER", true) || !order.stato.equals("SPEDITO", true)) {
            return
        }

        viewModelScope.launch {
            mutateState {
                it.copy(
                    pickupInProgressId = order.idOrdine,
                    pickupMessage = "Ritirato dal locker ${order.idLocker ?: ""}".trim()
                )
            }
            delay(15_000)
            val result = orderRepository.updateOrderStatus(order.idOrdine, "CONCLUSO")
            result.onSuccess {
                mutateState { it.copy(pickupInProgressId = null, pickupMessage = null) }
            }.onFailure { error ->
                mutateState {
                    it.copy(
                        pickupInProgressId = null,
                        pickupMessage = error.message ?: "Errore nel completamento ritiro"
                    )
                }
            }
        }
    }

    private fun filterProducts(state: CatalogUiState): List<Product> {
        val categoryMap = state.allCategories.associateBy { it.id }
        return state.allProducts
            .filter { product ->
                when {
                    state.selectedCategoryId != null -> product.categoryId == state.selectedCategoryId
                    state.selectedParentId != null -> categoryMap[product.categoryId]?.parentId == state.selectedParentId
                    else -> true
                }
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
                if (state.selectedTags.isEmpty()) true
                else {
                    val tags = parseTagsFromJson(product.tags)
                    state.selectedTags.all { it in tags }
                }
            }
    }

    private fun parseTagsFromJson(tags: List<String>?): List<String> = tags ?: emptyList()

    private fun orderTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(
                    raw.replace("T", " ").replace("Z", ""),
                    fallbackOrderFormatter
                ).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) {
                0L
            }
        }
    }

    private fun mutateState(transform: (CatalogUiState) -> CatalogUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            recomputeDerivedState(updated)
        }
    }

    private fun observeFavorites(userId: Int) {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            favoritesDataStore.favoritesFlow(userId).collect { ids ->
                mutateState { it.copy(favoriteProductIds = ids) }
            }
        }
    }

    private fun recomputeDerivedState(state: CatalogUiState): CatalogUiState {
        val visibleProducts = filterProducts(state)
        val cartItems = state.cart.mapNotNull { (productId, quantity) ->
            state.allProducts.firstOrNull { it.id == productId }?.let { product ->
                CartItemUi(product = product, quantity = quantity)
            }
        }
        val favoriteProducts = state.allProducts.filter { state.favoriteProductIds.contains(it.id) }
        val total = cartItems.sumOf { (it.product.price * it.quantity).toDouble() }
        val count = cartItems.sumOf { it.quantity }

        val parentNameFallback = mapOf(
            "1" to "Casa",
            "2" to "Cura Personale",
            "3" to "Carne",
            "4" to "Pesce",
            "5" to "Verdura",
            "6" to "Frutta",
            "7" to "Bevande"
        )

        val groupedCategories = state.allCategories
            .groupBy { it.parentId }

        val menuSections = groupedCategories
            .toSortedMap(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            .map { (parentId, categories) ->
                val title = categories.firstOrNull { !it.parentName.isNullOrBlank() }?.parentName
                    ?: parentNameFallback[parentId]
                    ?: "Altro"
                SideMenuSection(
                    id = parentId ?: "parent-none",
                    parentId = parentId,
                    title = title,
                    entries = categories
                        .sortedBy { it.nome }
                        .map { category ->
                            SideMenuEntry(
                                id = category.id,
                                title = category.nome
                            )
                        }
                )
            }
        
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
            allAvailableTags = allAvailableTags,
            favoriteProducts = favoriteProducts
        )
    }

    private fun lockerCodeFor(orderId: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rng = Random(orderId.toLong() * 1103515245 + 12345)
        return buildString {
            repeat(8) { append(chars[rng.nextInt(chars.length)]) }
        }
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
        if (snapshot.deliveryMethod == DeliveryMethod.DOMICILIO && snapshot.shippingAddress.isBlank()) {
            mutateState { it.copy(orderError = "Aggiungi un indirizzo di spedizione nelle impostazioni") }
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
                    metodoConsegna = snapshot.deliveryMethod.apiValue,
                    indirizzoSpedizione = snapshot.shippingAddress.ifBlank { null },
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
    val selectedParentId: String? = null,
    val selectedCategoryId: String? = null,
    val onlyOffers: Boolean = false,
    val selectedTags: Set<String> = emptySet(),
    val availabilityFilter: AvailabilityFilter = AvailabilityFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSubmittingOrder: Boolean = false,
    val orderError: String? = null,
    val lastOrderId: Int? = null,
    val orderHistory: List<CustomerOrderHistoryEntry> = emptyList(),
    val isOrderHistoryLoading: Boolean = false,
    val orderHistoryError: String? = null,
    val shippingAddress: String = "",
    val shippingPhone: String = "",
    val pickupInProgressId: Int? = null,
    val pickupMessage: String? = null,
    val showToast: Boolean = false,
    val toastMessage: String? = null,
    val favoriteProductIds: Set<String> = emptySet(),
    val favoriteProducts: List<Product> = emptyList(),
    val cart: Map<String, Int> = emptyMap(),
    val cartItems: List<CartItemUi> = emptyList(),
    val cartItemsCount: Int = 0,
    val cartTotal: Double = 0.0,
    val selectedProduct: Product? = null,
    val deliveryMethod: DeliveryMethod = DeliveryMethod.LOCKER
)

enum class AvailabilityFilter(val label: String) {
    ALL("Tutto"),
    ONLY_AVAILABLE("Solo disponibili"),
    INCLUDING_LOW_STOCK("Disponibili + in esaurimento")
}

enum class DeliveryMethod(val label: String, val apiValue: String) {
    LOCKER("Ritiro nel locker", "LOCKER"),
    DOMICILIO("Spesa a domicilio", "DOMICILIO");

    companion object {
        fun fromApi(value: String?): DeliveryMethod =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: LOCKER
    }
}

data class CartItemUi(
    val product: Product,
    val quantity: Int
)

data class CustomerOrderHistoryEntry(
    val order: Order,
    val sequenceNumber: Int
)












