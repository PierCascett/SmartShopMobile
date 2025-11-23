/**
 * EmployeeViewModel.kt
 *
 * Gestisce la schermata Dipendente:
 * - osserva ordini dal repository
 * - carica scaffali e prodotti e li mappa alle corsie della mappa (poligono N -> scaffale id N)
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.repository.CategoryRepository
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

    fun selectAisle(aisleId: String) {
        Log.d("EmployeeVM", "selectAisle -> $aisleId")
        _uiState.update { it.copy(selectedAisleId = aisleId) }
    }

    fun startOrder(orderId: Int) {
        _uiState.update { it.copy(activeOrderId = orderId, pickedLines = emptySet()) }
    }

    fun dropActiveOrder() {
        _uiState.update { it.copy(activeOrderId = null, pickedLines = emptySet()) }
    }

    fun togglePicked(lineId: Int) {
        _uiState.update { state ->
            val updated = state.pickedLines.toMutableSet().apply {
                if (contains(lineId)) remove(lineId) else add(lineId)
            }
            state.copy(pickedLines = updated)
        }
    }

    fun toggleOrder(orderId: Int) {
        _uiState.update { state ->
            state.copy(
                expandedOrderId = if (state.expandedOrderId == orderId) null else orderId,
                orderActionError = null
            )
        }
    }

    fun setOrderFilter(filter: OrderFilter) {
        _uiState.update { it.copy(orderFilter = filter, expandedOrderId = null, orderActionError = null) }
    }

    fun markOrderShipped(orderId: Int) {
        updateOrderStatus(orderId, "SPEDITO")
    }

    fun markOrderCompleted(orderId: Int) {
        val order = _uiState.value.orders.firstOrNull { it.idOrdine == orderId }
        val targetStatus = if (order?.metodoConsegna.equals("DOMICILIO", true)) {
            "CONSEGNATO"
        } else {
            "SPEDITO"
        }
        updateOrderStatus(orderId, targetStatus)
    }

    fun markOrderCanceled(orderId: Int) = updateOrderStatus(orderId, "ANNULLATO")

    fun showProductDetail(product: AisleProduct) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun dismissProductDetail() {
        _uiState.update { it.copy(selectedProduct = null) }
    }

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
