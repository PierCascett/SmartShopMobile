/**
 * MainViewModel.kt
 *
 * MVVM: ViewModel Layer - Gestione stato principale applicazione
 *
 * FUNZIONAMENTO:
 * - Centralizza stato navigazione e sessione utente
 * - Coordina LoginViewModel e CatalogViewModel
 * - Gestisce ripristino sessione automatico
 * - Espone stato UI unificato per MainActivity
 *
 * PATTERN MVVM:
 * - ViewModel: logica navigazione e coordinamento
 * - StateFlow: stato reattivo per UI
 * - viewModelScope: gestione coroutines lifecycle-aware
 * - Single Source of Truth per stato applicazione
 *
 * RESPONSABILITÀ:
 * - Gestione sessione utente (login/logout)
 * - Stato navigazione (showMenu, showCart, currentTab)
 * - Coordinamento tra ViewModel
 * - Toast e messaggi UI
 */
package it.unito.smartshopmobile.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CustomerTab { SHOP, ORDERS, ACCOUNT }

data class MainUiState(
    val loggedUser: User? = null,
    val selectedRole: UserRole? = null,
    val showMenu: Boolean = false,
    val showCart: Boolean = false,
    val showFavorites: Boolean = false,
    val currentTab: CustomerTab = CustomerTab.SHOP,
    val toastMessage: String? = null,
    val employeeProfileTrigger: Int = 0,
    val managerProfileTrigger: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionDataStore = SessionDataStore(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Ripristino automatico sessione
        viewModelScope.launch {
            sessionDataStore.userFlow.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            loggedUser = user,
                            selectedRole = UserRole.fromDbRole(user.ruolo)
                        )
                    }
                }
            }
        }
    }

    fun onLoginSuccess(user: User, role: UserRole) {
        _uiState.update {
            it.copy(
                loggedUser = user,
                selectedRole = role,
                toastMessage = "Accesso: ${user.email}"
            )
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            sessionDataStore.clear()
            _uiState.update {
                MainUiState() // Reset completo stato
            }
        }
    }

    fun setShowMenu(show: Boolean) {
        _uiState.update { it.copy(showMenu = show) }
    }

    fun setShowCart(show: Boolean) {
        _uiState.update { it.copy(showCart = show) }
    }

    fun setShowFavorites(show: Boolean) {
        _uiState.update { it.copy(showFavorites = show) }
    }

    fun setCurrentTab(tab: CustomerTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun openAccountForRole(role: UserRole?) {
        when (role) {
            UserRole.CUSTOMER -> setCurrentTab(CustomerTab.ACCOUNT)
            UserRole.EMPLOYEE -> _uiState.update {
                it.copy(employeeProfileTrigger = it.employeeProfileTrigger + 1)
            }
            UserRole.MANAGER -> _uiState.update {
                it.copy(managerProfileTrigger = it.managerProfileTrigger + 1)
            }
            null -> Unit
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}

