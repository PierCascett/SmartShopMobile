/**
 * EmployeeViewModel.kt
 *
 * RUOLO MVVM: ViewModel Layer
 * - Gestisce lo stato UI e la logica della schermata Dipendente
 * - Coordina operazioni specifiche per il ruolo Employee
 * - Intermedia tra UI (EmployeeScreen) e Repository (futuro OrderRepository)
 *
 * RESPONSABILITÀ (future):
 * - Gestire lista ordini in tempo reale
 * - Assegnare locker ai clienti
 * - Gestire consegne
 * - Aggiornare stato ordini
 * - Notifiche per nuovi ordini
 *
 * PATTERN: MVVM (Model-View-ViewModel)
 * - Stato osservabile (StateFlow/MutableState)
 * - Intent utente (assignLocker, completeDelivery)
 * - Usa OrderRepository per accesso dati
 *
 * ESEMPIO (futuro):
 * class EmployeeViewModel(
 *     private val orderRepository: OrderRepository
 * ) : ViewModel() {
 *     val orders = orderRepository.observePendingOrders()
 *         .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
 * }
 */
package it.unito.smartshopmobile.viewModel

import androidx.lifecycle.ViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Modelli per la mappa del supermercato
data class StoreAisle(
    val id: String,
    val name: String,
    val description: String,
    val products: List<AisleProduct>
)

data class AisleProduct(
    val name: String,
    val price: Double,
    val tags: List<String>
)

data class EmployeeUiState(
    val selectedAisleId: String? = null,
    val aisles: Map<String, StoreAisle> = emptyMap()
) {
    val selectedAisle: StoreAisle?
        get() = selectedAisleId?.let { aisles[it] }
}

class EmployeeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeUiState(aisles = createStoreAisles()))
    val uiState: StateFlow<EmployeeUiState> = _uiState.asStateFlow()

    fun selectAisle(aisleId: String) {
        Log.d("EmployeeVM", "selectAisle -> $aisleId")
        _uiState.update { it.copy(selectedAisleId = aisleId) }
    }

    private fun createStoreAisles(): Map<String, StoreAisle> {
        return mapOf(
            "S1" to StoreAisle(
                id = "S1",
                name = "Scaffale 1",
                description = "Prodotti freschi e alimentari",
                products = listOf(
                    AisleProduct("Mele Golden", 2.49, listOf("Bio", "Fresco")),
                    AisleProduct("Pomodori", 1.99, listOf("Fresco")),
                    AisleProduct("Pasta Barilla", 1.49, listOf("Italiana")),
                    AisleProduct("Olio EVO", 6.99, listOf("Extravergine"))
                )
            ),
            "S2" to StoreAisle(
                id = "S2",
                name = "Scaffale 2",
                description = "Bevande e snack",
                products = listOf(
                    AisleProduct("Acqua naturale 6x1.5L", 2.49, listOf("Naturale")),
                    AisleProduct("Coca Cola", 3.99, listOf("Lattina")),
                    AisleProduct("Kinder Bueno", 2.49, listOf("Cioccolato")),
                    AisleProduct("Patatine Lay's", 1.99, listOf("Salato"))
                )
            ),
            "S3" to StoreAisle(
                id = "S3",
                name = "Scaffale 3",
                description = "Prodotti per la casa",
                products = listOf(
                    AisleProduct("Dash liquido", 5.99, listOf("Lavatrice")),
                    AisleProduct("Scottex", 3.99, listOf("Carta")),
                    AisleProduct("Shampoo Pantene", 4.49, listOf("Capelli")),
                    AisleProduct("Dentifricio AZ", 2.29, listOf("Igiene"))
                )
            ),
            "S4" to StoreAisle(
                id = "S4",
                name = "Scaffale 4",
                description = "Surgelati e latticini",
                products = listOf(
                    AisleProduct("Pizza margherita", 3.49, listOf("Surgelato")),
                    AisleProduct("Gelato Magnum", 4.99, listOf("Premium")),
                    AisleProduct("Latte fresco", 1.29, listOf("Fresco")),
                    AisleProduct("Parmigiano DOP", 15.99, listOf("DOP"))
                )
            )
        )
    }
}
