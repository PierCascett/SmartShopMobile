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

// EmployeeViewModel

