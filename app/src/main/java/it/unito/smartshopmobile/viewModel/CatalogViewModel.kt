/**
 * CatalogViewModel.kt
 *
 * MVVM: ViewModel Layer - Gestione catalogo prodotti
 *
 * FUNZIONAMENTO:
 * - Gestisce stato UI complesso (prodotti, categorie, carrello, ordini)
 * - Coordina multiple Repository (Product, Category, Shelf, Order)
 * - Espone StateFlow<CatalogUiState> con stato unificato e immutabile
 * - Sincronizza dati da API e osserva database locale (Room)
 *
 * PATTERN MVVM:
 * - ViewModel: logica presentazione, sopravvive a rotazioni schermo
 * - StateFlow: stato reattivo type-safe (no dipendenze Compose)
 * - Coroutines: operazioni asincrone con viewModelScope
 * - combine(): osserva multiple Flow simultaneamente
 * - Unidirectional Data Flow: UI → Intent → ViewModel → State → UI
 *
 * BEST PRACTICE RISPETTATE:
 * - Single Source of Truth: uiState contiene tutto lo stato
 * - Immutabilità: CatalogUiState è data class immutabile
 * - Separation of Concerns: NON accede direttamente a Room/Retrofit
 * - Repository Pattern: delega logica dati ai Repository
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.datastore.FavoritesDataStore
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.CreateOrderRequest
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.OrderItemRequest
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.UpdateUserRequest
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.repository.UserRepository
import it.unito.smartshopmobile.ui.screens.SideMenuEntry
import it.unito.smartshopmobile.ui.screens.SideMenuSection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/**
 * ViewModel principale per il dominio Customer: gestisce catalogo, carrello, ordini e preferiti.
 *
 * Unifica stato locale (Room/DataStore) e remoto (Retrofit) in un unico `CatalogUiState` esposto
 * via `StateFlow`, si occupa di sincronizzazione dati, gestione overlay (carrello/preferiti),
 * storico ordini e aggiornamento profilo utente, rispettando un flusso unidirezionale di eventi.
 */
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
    /**
     * Helper per gestire observe data.
     */

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
    /**
     * Helper per gestire observe order history.
     */

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
    /**
     * Helper per gestire merge products by id.
     */

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

    /** Memorizza l'utente loggato e avvia l'osservazione dei preferiti. */
    fun setLoggedUser(user: User) {
        mutateState { it.copy(loggedUser = user) }
        observeFavorites(user.id)
    }

    /** Pulisce la sessione e azzera lo stato (carrello, preferiti, utente). */
    fun clearSession() {
        started = false
        favoritesJob?.cancel()
        favoritesJob = null
        mutateState { CatalogUiState() }
    }

    /**
     * Aggiorna i contatti cliente (indirizzo/telefono) usati per ordini.
     * Non effettua persistenza remota.
     */
    fun setCustomerContacts(indirizzo: String, telefono: String?) {
        mutateState {
            it.copy(
                shippingAddress = indirizzo,
                shippingPhone = telefono.orEmpty()
            )
        }
    }

    /**
     * Aggiorna il profilo utente sul backend e nello stato locale.
     * Restituisce Result per consentire feedback in UI.
     */
    suspend fun updateUserProfile(
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
    /**
     * Helper per gestire update customer profile.
     */

    @Deprecated("Usa updateUserProfile per qualsiasi ruolo")
    suspend fun updateCustomerProfile(
        nome: String,
        cognome: String,
        email: String,
        telefono: String?
    ): Result<User> = updateUserProfile(nome, cognome, email, telefono)

    /** Carica una foto profilo e aggiorna l'avatar dell'utente loggato. */
    suspend fun uploadProfilePhoto(uri: android.net.Uri): Result<String> {
        val current = _uiState.value.loggedUser ?: return Result.failure(Exception("Utente non loggato"))
        return try {
            val resolver = getApplication<Application>().contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Impossibile leggere l'immagine selezionata"))
            val uploadResult = userRepository.uploadAvatar(current.id, bytes, mimeType)
            uploadResult.onSuccess { url ->
                val cacheBustedUrl = if (url.contains("?")) "$url&ts=${System.currentTimeMillis()}" else "$url?ts=${System.currentTimeMillis()}"
                val updatedUser = current.copy(avatarUrl = cacheBustedUrl)
                mutateState { it.copy(loggedUser = updatedUser) }
                sessionDataStore.saveUser(updatedUser)
            }
            uploadResult
        } catch (e: Exception) {
            Log.e("CatalogViewModel", "Errore upload foto profilo", e)
            Result.failure(e)
        }
    }
    /**
     * Helper per gestire refresh data.
     */

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

    /** Ritenta il recupero dati catalogo dopo un errore. */
    fun retry() {
        refreshData()
    }

    /** Richiama il refresh del catalogo (alias di retry). */
    fun refreshCatalog() {
        refreshData()
    }
    /**
     * Helper per gestire on search query change.
     */

    fun onSearchQueryChange(query: String) = mutateState {
        it.copy(searchQuery = query)
    }
    /**
     * Helper per gestire on parent category selected.
     */

    fun onParentCategorySelected(parentId: String?) = mutateState {
        it.copy(selectedParentId = parentId, selectedCategoryId = null)
    }
    /**
     * Helper per gestire on category selected.
     */

    fun onCategorySelected(categoryId: String?) = mutateState { current ->
        val parentId = categoryId?.let { id ->
            current.allCategories.firstOrNull { cat -> cat.id == id }?.parentId
        }
        current.copy(selectedCategoryId = categoryId, selectedParentId = parentId)
    }
    /**
     * Helper per gestire on product selected.
     */

    fun onProductSelected(productId: String?) = mutateState { state ->
        val product = state.allProducts.firstOrNull { it.id == productId }
        state.copy(selectedProduct = product)
    }
    /**
     * Helper per gestire on only offers toggle.
     */

    fun onOnlyOffersToggle() = mutateState { current ->
        current.copy(onlyOffers = !current.onlyOffers)
    }
    /**
     * Helper per gestire on availability filter change.
     */

    fun onAvailabilityFilterChange(filter: AvailabilityFilter) = mutateState {
        it // availability filter disabilitato
    }
    /**
     * Helper per gestire on tag toggle.
     */

    fun onTagToggle(tag: String) = mutateState { state ->
        val updated = state.selectedTags.toMutableSet().apply {
            if (contains(tag)) remove(tag) else add(tag)
        }
        state.copy(selectedTags = updated)
    }
    /**
     * Helper per gestire on bookmark.
     */

    fun onBookmark(productId: String) {
        val userId = _uiState.value.loggedUser?.id ?: return
        viewModelScope.launch {
            val current = _uiState.value.favoriteProductIds
            val updated = if (current.contains(productId)) current - productId else current + productId
            mutateState { it.copy(favoriteProductIds = updated) }
            favoritesDataStore.saveFavorites(userId, updated)
        }
    }
    /**
     * Helper per gestire on add to cart.
     */

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
    /**
     * Helper per gestire on decrease cart item.
     */

    fun onDecreaseCartItem(productId: String) = mutateState { state ->
        val currentQuantity = state.cart[productId] ?: return@mutateState state
        val updatedCart = state.cart.toMutableMap().apply {
            if (currentQuantity <= 1) remove(productId) else put(productId, currentQuantity - 1)
        }
        state.copy(cart = updatedCart.toMap())
    }
    /**
     * Helper per gestire on remove from cart.
     */

    fun onRemoveFromCart(productId: String) = mutateState { state ->
        if (!state.cart.containsKey(productId)) return@mutateState state
        val updatedCart = state.cart.toMutableMap().apply { remove(productId) }
        state.copy(cart = updatedCart.toMap())
    }
    /**
     * Helper per gestire on delivery method selected.
     */

    fun onDeliveryMethodSelected(method: DeliveryMethod) = mutateState { state ->
        state.copy(deliveryMethod = method)
    }

    /** Consuma il toast corrente azzerando flag e messaggio. */
    fun consumeToast() = mutateState { it.copy(showToast = false, toastMessage = null) }

    /** Pulisce l'ID dell'ultimo ordine inviato. */
    fun clearOrderFeedback() = mutateState { it.copy(lastOrderId = null) }

    /** Pulisce eventuali messaggi di ritiro da locker. */
    fun clearPickupMessage() = mutateState { it.copy(pickupMessage = null) }

    /** Aggiorna lo storico ordini dal backend per l'utente loggato. */
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

    /**
     * Simula il ritiro di un ordine LOCKER: mostra messaggio e prova a settare lo stato su CONCLUSO.
     * Evita esecuzioni multiple se un pickup è già in corso.
     */
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
    /**
     * Helper per gestire filter products.
     */

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
    /**
     * Helper per gestire parse tags from json.
     */

    private fun parseTagsFromJson(tags: List<String>?): List<String> = tags ?: emptyList()
    /**
     * Helper per gestire order timestamp.
     */

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
    /**
     * Helper per gestire mutate state.
     */

    private fun mutateState(transform: (CatalogUiState) -> CatalogUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            recomputeDerivedState(updated)
        }
    }
    /**
     * Helper per gestire observe favorites.
     */

    private fun observeFavorites(userId: Int) {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            favoritesDataStore.favoritesFlow(userId).collect { ids ->
                mutateState { it.copy(favoriteProductIds = ids) }
            }
        }
    }
    /**
     * Helper per gestire recompute derived state.
     */

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

        val groupedCategories = state.allCategories
            .groupBy { it.parentId }

        val menuSections = groupedCategories
            .toSortedMap(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            .map { (parentId, categories) ->
                val title = categories
                    .mapNotNull { it.parentName?.takeIf { name -> name.isNotBlank() } }
                    .firstOrNull()
                    ?: parentId
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
    /**
     * Helper per gestire locker code for.
     */

    private fun lockerCodeFor(orderId: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rng = Random(orderId.toLong() * 1103515245 + 12345)
        return buildString {
            repeat(8) { append(chars[rng.nextInt(chars.length)]) }
        }
    }

    /**
     * Invia un ordine usando il carrello corrente e il metodo di consegna selezionato.
     * Valida utente, carrello non vuoto e indirizzo per consegna a domicilio prima di procedere.
     */
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
        /**
         * Helper per gestire from api.
         */
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





