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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManagerUiState(
    val restocks: List<Restock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val prodottoId: String = "",
    val fornitoreId: String = "",
    val quantita: String = "",
    val dataArrivoPrevista: String = "",
    val successMessage: String? = null
)

class ManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RestockRepository(
        RetrofitInstance.api,
        SmartShopDatabase.getDatabase(application).restockDao()
    )
    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState: StateFlow<ManagerUiState> = _uiState.asStateFlow()

    init {
        observeRestocks()
        refreshRestocks()
    }

    private fun observeRestocks() {
        viewModelScope.launch {
            repository.observeRestocks().collect { list ->
                _uiState.update { it.copy(restocks = list) }
            }
        }
    }

    fun refreshRestocks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.fetchRestocks()
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    fun updateForm(productId: String? = null, supplierId: String? = null, quantity: String? = null, eta: String? = null) {
        _uiState.update {
            it.copy(
                prodottoId = productId ?: it.prodottoId,
                fornitoreId = supplierId ?: it.fornitoreId,
                quantita = quantity ?: it.quantita,
                dataArrivoPrevista = eta ?: it.dataArrivoPrevista
            )
        }
    }

    fun submitRestock(responsabileId: Int? = null) {
        val state = _uiState.value
        val qty = state.quantita.toIntOrNull()
        val supplier = state.fornitoreId.toIntOrNull()
        if (state.prodottoId.isBlank() || supplier == null || qty == null) {
            _uiState.update { it.copy(error = "Compila id prodotto, fornitore e quantit\u00e0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val request = CreateRestockRequest(
                idProdotto = state.prodottoId.trim(),
                idFornitore = supplier,
                quantitaOrdinata = qty,
                dataArrivoPrevista = state.dataArrivoPrevista.ifBlank { null },
                idResponsabile = responsabileId
            )
            repository.createRestock(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Riordino creato (id ${it.prodottoId})",
                            prodottoId = "",
                            fornitoreId = "",
                            quantita = "",
                            dataArrivoPrevista = ""
                        )
                    }
                    refreshRestocks()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
