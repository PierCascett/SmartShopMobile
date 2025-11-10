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
        _uiState.update { it.copy(selectedAisleId = aisleId) }
    }

    private fun createStoreAisles(): Map<String, StoreAisle> {
        return mapOf(
            "A1" to StoreAisle(
                id = "A1",
                name = "Frutta & Verdura",
                description = "Prodotti freschi e biologici",
                products = listOf(
                    AisleProduct("Mele Golden", 2.49, listOf("Bio", "Locale")),
                    AisleProduct("Pomodori", 1.99, listOf("Fresco")),
                    AisleProduct("Insalata", 0.99, listOf("Bio")),
                    AisleProduct("Banane", 1.59, listOf("Tropicale")),
                    AisleProduct("Arance", 2.29, listOf("Vitamina C"))
                )
            ),
            "A2" to StoreAisle(
                id = "A2",
                name = "Panetteria",
                description = "Pane fresco e dolci",
                products = listOf(
                    AisleProduct("Pane integrale", 1.29, listOf("Fresco", "Integrale")),
                    AisleProduct("Croissant", 0.89, listOf("Dolce")),
                    AisleProduct("Baguette", 0.99, listOf("Francese")),
                    AisleProduct("Ciambella", 1.49, listOf("Dolce"))
                )
            ),
            "A3" to StoreAisle(
                id = "A3",
                name = "Salumeria",
                description = "Salumi e formaggi",
                products = listOf(
                    AisleProduct("Prosciutto crudo", 12.99, listOf("Premium", "DOP")),
                    AisleProduct("Parmigiano", 15.99, listOf("DOP", "Stagionato")),
                    AisleProduct("Mortadella", 8.99, listOf("IGP")),
                    AisleProduct("Gorgonzola", 9.99, listOf("DOP"))
                )
            ),
            "B1" to StoreAisle(
                id = "B1",
                name = "Pasta & Riso",
                description = "Primi piatti e cereali",
                products = listOf(
                    AisleProduct("Pasta Barilla", 1.49, listOf("Italiana")),
                    AisleProduct("Riso Arborio", 2.99, listOf("Risotto")),
                    AisleProduct("Pasta integrale", 1.79, listOf("Integrale")),
                    AisleProduct("Couscous", 2.49, listOf("Orientale"))
                )
            ),
            "B2" to StoreAisle(
                id = "B2",
                name = "Conserve",
                description = "Passate e legumi",
                products = listOf(
                    AisleProduct("Passata pomodoro", 0.89, listOf("100% Italiano")),
                    AisleProduct("Ceci in scatola", 0.79, listOf("Legumi")),
                    AisleProduct("Tonno", 1.29, listOf("Pesce")),
                    AisleProduct("Fagioli", 0.69, listOf("Legumi"))
                )
            ),
            "B3" to StoreAisle(
                id = "B3",
                name = "Condimenti",
                description = "Olio, aceto e spezie",
                products = listOf(
                    AisleProduct("Olio EVO", 6.99, listOf("Extravergine", "Italiano")),
                    AisleProduct("Aceto balsamico", 3.49, listOf("Modena")),
                    AisleProduct("Sale marino", 0.59, listOf("Iodato")),
                    AisleProduct("Pepe nero", 1.99, listOf("Spezia"))
                )
            ),
            "C1" to StoreAisle(
                id = "C1",
                name = "Bevande",
                description = "Acqua, succhi e bibite",
                products = listOf(
                    AisleProduct("Acqua naturale 6x1.5L", 2.49, listOf("Naturale")),
                    AisleProduct("Coca Cola", 3.99, listOf("Lattina")),
                    AisleProduct("Succo arancia", 2.29, listOf("100% Frutta")),
                    AisleProduct("Tè freddo", 1.79, listOf("Limone"))
                )
            ),
            "C2" to StoreAisle(
                id = "C2",
                name = "Snack & Dolci",
                description = "Merendine e cioccolato",
                products = listOf(
                    AisleProduct("Kinder Bueno", 2.49, listOf("Cioccolato")),
                    AisleProduct("Patatine Lay's", 1.99, listOf("Salato")),
                    AisleProduct("Biscotti Mulino", 1.79, listOf("Colazione")),
                    AisleProduct("Nutella", 4.99, listOf("Cioccolato"))
                )
            ),
            "C3" to StoreAisle(
                id = "C3",
                name = "Surgelati",
                description = "Gelati e piatti pronti",
                products = listOf(
                    AisleProduct("Pizza margherita", 3.49, listOf("Surgelato")),
                    AisleProduct("Gelato Magnum", 4.99, listOf("Premium")),
                    AisleProduct("Verdure miste", 2.29, listOf("Surgelato", "Sano")),
                    AisleProduct("Lasagne", 5.99, listOf("Pronto"))
                )
            ),
            "D1" to StoreAisle(
                id = "D1",
                name = "Detersivi",
                description = "Pulizia casa",
                products = listOf(
                    AisleProduct("Dash liquido", 5.99, listOf("Lavatrice")),
                    AisleProduct("Mastro Lindo", 2.49, listOf("Pavimenti")),
                    AisleProduct("Scottex", 3.99, listOf("Carta")),
                    AisleProduct("Candeggina", 1.99, listOf("Igienizzante"))
                )
            ),
            "D2" to StoreAisle(
                id = "D2",
                name = "Igiene",
                description = "Cura personale",
                products = listOf(
                    AisleProduct("Shampoo Pantene", 4.49, listOf("Capelli")),
                    AisleProduct("Nivea crema", 3.99, listOf("Viso")),
                    AisleProduct("Dentifricio AZ", 2.29, listOf("Igiene orale")),
                    AisleProduct("Bagnoschiuma", 3.49, listOf("Doccia"))
                )
            ),
            "D3" to StoreAisle(
                id = "D3",
                name = "Pet Care",
                description = "Cibo per animali",
                products = listOf(
                    AisleProduct("Crocchette cane", 8.99, listOf("Cani")),
                    AisleProduct("Cibo gatti", 6.99, listOf("Gatti")),
                    AisleProduct("Lettiera", 4.49, listOf("Igiene")),
                    AisleProduct("Snack cane", 2.99, listOf("Premio"))
                )
            )
        )
    }
}

