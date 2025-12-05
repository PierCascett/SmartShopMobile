/**
 * EmployeeViewModel.kt
 *
 * MVVM: ViewModel Layer - Gestione schermata Dipendente
 *
 * FUNZIONAMENTO:
 * - Osserva ordini da OrderRepository per picking
 * - Carica scaffali e prodotti, mappa a corsie mappa (poligono N → scaffale N)
 * - Gestisce ordine attivo, linee picked, navigazione mappa
 * - Aggiorna stato ordini (SPEDITO, CONSEGNATO, ANNULLATO)
 *
 * PATTERN MVVM:
 * - ViewModel: logica presentazione per UI dipendente
 * - StateFlow<EmployeeUiState>: stato unificato e immutabile
 * - Coroutines: operazioni asincrone (update ordini, refresh)
 * - combine(): osserva ordini, scaffali, prodotti simultaneamente
 * - Repository Pattern: delega dati a OrderRepository, ShelfRepository, ProductRepository
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoreAisle(
    val id: String,
    val name: String,
    val description: String,
    val products: List<AisleProduct>
)

data class AisleProduct(
    val id: String,
    val name: String,
    val price: Double,
    val tags: List<String>,
    val brand: String,
    val description: String?,
    val imageUrl: String?,
    val shelfId: String
)

data class EmployeeUiState(
    val selectedAisleId: String? = null,
    val aisles: Map<String, StoreAisle> = emptyMap(),
    val orders: List<Order> = emptyList(),
    val isLoadingOrders: Boolean = false,
    val ordersError: String? = null,
    val isLoadingAisles: Boolean = true,
    val aislesError: String? = null,
    val expandedOrderId: Int? = null,
    val updatingOrderId: Int? = null,
    val orderActionError: String? = null,
    val orderFilter: OrderFilter = OrderFilter.ACTIVE,
    val selectedProduct: AisleProduct? = null,
    val activeOrderId: Int? = null,
    val pickedLines: Set<Int> = emptySet(),
    val productShelfMap: Map<String, String> = emptyMap()
) {
    val selectedAisle: StoreAisle?
        get() = selectedAisleId?.let { aisles[it] }
    val activeOrder: Order?
        get() = activeOrderId?.let { id -> orders.firstOrNull { it.idOrdine == id } }
}

enum class OrderFilter { ACTIVE, COMPLETED }

/**
 * ViewModel per il flusso dipendente (picking, consegne e navigazione corsie).
 *
 * Osserva ordini/scaffali/prodotti dai repository, mappa le corsie per la mappa interattiva
 * e gestisce intent UI come selezione corsia, marcatura righe evase e avanzamento stato ordine,
 * fornendo alla UI uno `StateFlow<EmployeeUiState>` unico e coerente.
 */
class EmployeeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SmartShopDatabase.getDatabase(application)
    private val orderRepository = OrderRepository(
        RetrofitInstance.api,
        database.orderDao()
    )
    private val productRepository = ProductRepository(
        database.productDao(),
        RetrofitInstance.api
    )
    private val shelfRepository = ShelfRepository(
        database.shelfDao(),
        RetrofitInstance.api
    )
    private val categoryRepository = CategoryRepository(
        database.categoryDao(),
        RetrofitInstance.api
    )

    private val _uiState = MutableStateFlow(EmployeeUiState())
    val uiState: StateFlow<EmployeeUiState> = _uiState.asStateFlow()

    init {
        observeOrders()
        observeAisles()
        refreshShelvesAndProducts()
        refreshOrders()
    }
    /**
     * Helper per gestire observe orders.
     */

    private fun observeOrders() {
        viewModelScope.launch {
            orderRepository.observeOrders().collect { ordersWithLines ->
                _uiState.update { state ->
                    val mappedOrders = ordersWithLines.map { it.order.copy(righe = it.lines) }
                    val active = state.activeOrderId?.takeIf { id -> mappedOrders.any { it.idOrdine == id } }
                    val picked = state.pickedLines.filter { lineId ->
                        val activeOrder = mappedOrders.firstOrNull { it.idOrdine == active }
                        activeOrder?.righe?.any { it.idRiga == lineId } == true
                    }.toSet()
                    state.copy(
                        orders = mappedOrders,
                        activeOrderId = active,
                        pickedLines = picked
                    )
                }
            }
        }
    }
    /**
     * Helper per gestire observe aisles.
     */

    private fun observeAisles() {
        viewModelScope.launch {
            combine(
                shelfRepository.getAll(),
                productRepository.getAllProducts()
            ) { shelves, products ->
                shelves to products
            }.collect { (shelves, products) ->
                val productShelfMap = mutableMapOf<String, String>()
                val aisles = shelves.associate { shelf ->
                    val aisleProducts = products
                        .filter { it.shelfId == shelf.id }
                        .map { product ->
                            productShelfMap[product.id] = shelf.id.toString()
                            AisleProduct(
                                id = product.id,
                                name = product.name,
                                price = product.price,
                                tags = product.tags ?: emptyList(),
                                brand = product.brand,
                                description = product.description,
                                imageUrl = product.imageUrl,
                                shelfId = shelf.id.toString()
                            )
                        }
                    shelf.id.toString() to StoreAisle(
                        id = shelf.id.toString(),
                        name = shelf.nome,
                        description = shelf.descrizione ?: "Scaffale ${shelf.id}",
                        products = aisleProducts
                    )
                }
                _uiState.update { state ->
                    val selected =
                        state.selectedAisleId?.takeIf { aisles.containsKey(it) }
                    state.copy(
                        aisles = aisles,
                        selectedAisleId = selected,
                        isLoadingAisles = false,
                        aislesError = null,
                        productShelfMap = productShelfMap
                    )
                }
            }
        }
    }

    /** Seleziona una corsia sulla mappa e aggiorna lo stato UI. */
    fun selectAisle(aisleId: String) {
        Log.d("EmployeeVM", "selectAisle -> $aisleId")
        _uiState.update { it.copy(selectedAisleId = aisleId) }
    }

    /** Imposta l'ordine attivo su cui l'operatore sta lavorando. */
    fun startOrder(orderId: Int) {
        _uiState.update { it.copy(activeOrderId = orderId, pickedLines = emptySet()) }
    }

    /** Annulla l'ordine attivo e svuota la checklist di picking. */
    fun dropActiveOrder() {
        _uiState.update { it.copy(activeOrderId = null, pickedLines = emptySet()) }
    }

    /** Aggiunge/rimuove una riga ordine dalla checklist di picking. */
    fun togglePicked(lineId: Int) {
        _uiState.update { state ->
            val updated = state.pickedLines.toMutableSet().apply {
                if (contains(lineId)) remove(lineId) else add(lineId)
            }
            state.copy(pickedLines = updated)
        }
    }

    /** Espande o comprime i dettagli di un ordine nella lista. */
    fun toggleOrder(orderId: Int) {
        _uiState.update { state ->
            state.copy(
                expandedOrderId = if (state.expandedOrderId == orderId) null else orderId,
                orderActionError = null
            )
        }
    }

    /** Applica il filtro di stato (attivi/completati) agli ordini. */
    fun setOrderFilter(filter: OrderFilter) {
        _uiState.update { it.copy(orderFilter = filter, expandedOrderId = null, orderActionError = null) }
    }

    /** Segna un ordine come spedito. */
    fun markOrderShipped(orderId: Int) {
        updateOrderStatus(orderId, "SPEDITO")
    }

    /** Segna un ordine come completato (consegna o locker a seconda del metodo). */
    fun markOrderCompleted(orderId: Int) {
        val order = _uiState.value.orders.firstOrNull { it.idOrdine == orderId }
        val targetStatus = if (order?.metodoConsegna.equals("DOMICILIO", true)) {
            "CONSEGNATO"
        } else {
            "SPEDITO"
        }
        updateOrderStatus(orderId, targetStatus)
    }

    /** Segna un ordine come annullato. */
    fun markOrderCanceled(orderId: Int) = updateOrderStatus(orderId, "ANNULLATO")

    /** Mostra il dettaglio di un prodotto della corsia selezionata. */
    fun showProductDetail(product: AisleProduct) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    /** Chiude il dettaglio prodotto. */
    fun dismissProductDetail() {
        _uiState.update { it.copy(selectedProduct = null) }
    }
    /**
     * Helper per gestire update order status.
     */

    private fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingOrderId = orderId, orderActionError = null) }
            val result = orderRepository.updateOrderStatus(orderId, newStatus)
            result.onSuccess {
                _uiState.update { it.copy(updatingOrderId = null, orderActionError = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(updatingOrderId = null, orderActionError = error.message) }
            }
        }
    }

    /** Ricarica gli ordini dal backend e aggiorna i flag di loading/errore. */
    fun refreshOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrders = true, ordersError = null, orderActionError = null) }
            val result = orderRepository.refreshOrders()
            result.onSuccess {
                _uiState.update { it.copy(isLoadingOrders = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingOrders = false, ordersError = error.message) }
            }
        }
    }

    /** Ricarica scaffali e prodotti dal backend/Room e resetta eventuali errori. */
    fun refreshShelvesAndProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAisles = true, aislesError = null) }
            // Ordine: categorie -> scaffali -> prodotti (i prodotti hanno FK su categorie)
            val categoriesResult = categoryRepository.refreshCategories()
            val shelfResult = shelfRepository.refresh()
            val productsResult = productRepository.refreshProducts()
            val error = categoriesResult.exceptionOrNull()
                ?: shelfResult.exceptionOrNull()
                ?: productsResult.exceptionOrNull()
            _uiState.update {
                it.copy(
                    isLoadingAisles = false,
                    aislesError = error?.message
                )
            }
        }
    }
}
