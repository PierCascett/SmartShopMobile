/**
 * MainActivity.kt
 *
 * RUOLO MVVM: Entry Point & Navigation Host
 * - Activity principale dell'applicazione Android
 * - Ospita la navigazione tra le diverse schermate
 * - Inizializza il tema Material Design
 * - Gestisce il ciclo di vita dell'app
 *
 * RESPONSABILITÀ:
 * - Setup del tema (enableEdgeToEdge, MaterialTheme)
 * - Inizializzazione ViewModels (by viewModels())
 * - Gestione navigazione tra Login/Customer/Employee/Manager
 * - Gestione stato globale (drawer menu, carrello)
 * - Rispondere a cambiamenti di configurazione (rotazione)
 *
 * PATTERN: Controller (Android)
 * - ComponentActivity: ciclo di vita Android
 * - setContent: entry point Jetpack Compose
 * - viewModels(): ViewModel lifecycle-aware
 * - Navigation: routing condizionale in base a UserRole
 *
 * COMPONENTI:
 * - SmartShopApp: Composable root dell'app
 * - Routing: switch tra schermate in base al ruolo
 * - Theme wrapping: MaterialTheme applicato globalmente
 */
// Activity principale: avvio tema e schermata di login
// - uso `LoginViewModel` con `by viewModels()`
// - mostro `LoginScreenMVVM` e gestisco il successo con un Toast

package it.unito.smartshopmobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import it.unito.smartshopmobile.data.model.UserRole
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.ui.screens.CatalogScreen
import it.unito.smartshopmobile.ui.screens.LoginScreenMVVM
import it.unito.smartshopmobile.ui.screens.LoginScreen
import it.unito.smartshopmobile.ui.screens.EmployeeScreen
import it.unito.smartshopmobile.ui.screens.ManagerScreen
import it.unito.smartshopmobile.ui.screens.SideMenuOverlay
import it.unito.smartshopmobile.ui.screens.AppCartOverlay
import it.unito.smartshopmobile.ui.screens.AppOrderHistoryOverlay
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.CatalogViewModel
import it.unito.smartshopmobile.viewModel.CatalogUiState
import it.unito.smartshopmobile.viewModel.LoginViewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val catalogViewModel: CatalogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartShopMobileTheme {
                var loggedUser by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
                var showMenu by rememberSaveable { mutableStateOf(false) }
                var showCart by rememberSaveable { mutableStateOf(false) }
                var showOrderHistory by rememberSaveable { mutableStateOf(false) }
                val catalogState by catalogViewModel.uiState.collectAsState()
                val sessionUser by loginViewModel.sessionUser.collectAsState(initial = null)
                var sessionRestored by rememberSaveable { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val contentModifier = Modifier.padding(innerPadding)
                        if (loggedUser == null) {
                            if (!sessionRestored && sessionUser != null) {
                                sessionRestored = true
                                val saved = sessionUser!!
                                catalogViewModel.setLoggedUser(saved)
                                loggedUser = saved.email
                                selectedRole = UserRole.fromDbRole(saved.ruolo)
                                if (selectedRole == UserRole.CUSTOMER) {
                                    catalogViewModel.startSyncIfNeeded()
                                }
                            }
                            LoginScreenMVVM(
                                viewModel = loginViewModel,
                                modifier = contentModifier,
                                onLoginSuccess = { user, role ->
                                    catalogViewModel.setLoggedUser(user)
                                    loggedUser = user.email
                                    selectedRole = role
                                    if (role == UserRole.CUSTOMER) {
                                        catalogViewModel.startSyncIfNeeded()
                                    }
                                    Toast.makeText(this@MainActivity, "Accesso: ${user.email}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            ContentWithSessionBar(
                                modifier = contentModifier,
                                email = loggedUser ?: "",
                                role = selectedRole,
                                onLogout = {
                                    loggedUser = null
                                    selectedRole = null
                                    showCart = false
                                    showOrderHistory = false
                                    catalogViewModel.clearSession()
                                    loginViewModel.clearSession()
                                },
                                catalogState = catalogState,
                                catalogViewModel = catalogViewModel,
                                onMenuClick = { showMenu = true },
                                onCartClick = { showCart = true },
                                onHistoryClick = { showOrderHistory = true }
                            )
                        }
                    }

                    if (showMenu) {
                        SideMenuOverlay(
                            onDismiss = { showMenu = false },
                            sections = catalogState.sideMenuSections, // <-- usa categorie dal DB
                            onParentSelected = { parentId: String? ->
                                catalogViewModel.onParentCategorySelected(parentId)
                                catalogViewModel.onSearchQueryChange("")
                            },
                            onEntrySelected = { selection: String? ->
                                showMenu = false
                                catalogViewModel.onCategorySelected(selection)
                                catalogViewModel.onSearchQueryChange("")
                            }
                        )
                    }

                    if (showCart) {
                        AppCartOverlay(
                            onDismiss = {
                                showCart = false
                                catalogViewModel.clearOrderFeedback()
                            },
                            cartItems = catalogState.cartItems,
                            cartItemsCount = catalogState.cartItemsCount,
                            total = catalogState.cartTotal,
                            isSubmittingOrder = catalogState.isSubmittingOrder,
                            orderError = catalogState.orderError,
                            lastOrderId = catalogState.lastOrderId,
                            deliveryMethod = catalogState.deliveryMethod,
                            onIncrease = { id -> catalogViewModel.onAddToCart(id) },
                            onDecrease = { id -> catalogViewModel.onDecreaseCartItem(id) },
                            onRemove = { id -> catalogViewModel.onRemoveFromCart(id) },
                            onSubmitOrder = { catalogViewModel.submitOrder() },
                            onDeliveryMethodChange = { catalogViewModel.onDeliveryMethodSelected(it) }
                        )
                    }

                    LaunchedEffect(showOrderHistory) {
                        if (showOrderHistory) {
                            catalogViewModel.refreshOrderHistory()
                        }
                    }

                    if (showOrderHistory) {
                        AppOrderHistoryOverlay(
                            onDismiss = { showOrderHistory = false },
                            orders = catalogState.orderHistory,
                            isLoading = catalogState.isOrderHistoryLoading,
                            error = catalogState.orderHistoryError,
                            onRefresh = { catalogViewModel.refreshOrderHistory() }
                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun ContentWithSessionBar(
    modifier: Modifier,
    email: String,
    role: UserRole?,
    onLogout: () -> Unit,
    catalogState: CatalogUiState,
    catalogViewModel: CatalogViewModel,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SessionBar(
            email = email,
            onLogout = onLogout
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxSize()) {
                    when (role) {
                        UserRole.CUSTOMER -> CatalogScreen(
                            state = catalogState,
                            modifier = Modifier.fillMaxSize(),
                            onMenuClick = onMenuClick,
                            onCartClick = onCartClick,
                            onHistoryClick = onHistoryClick,
                            onSearchQueryChange = catalogViewModel::onSearchQueryChange,
                            onRefresh = catalogViewModel::refreshCatalog,
                            onToggleOffers = catalogViewModel::onOnlyOffersToggle,
                            onAvailabilityFilterChange = catalogViewModel::onAvailabilityFilterChange,
                            onTagToggle = catalogViewModel::onTagToggle,
                            onBookmark = catalogViewModel::onBookmark,
                            onAddToCart = catalogViewModel::onAddToCart,
                            onDecreaseCartItem = catalogViewModel::onDecreaseCartItem,
                            onRemoveFromCart = catalogViewModel::onRemoveFromCart,
                            onProductClick = catalogViewModel::onProductSelected
                        ).also {
                            if (catalogState.showToast && catalogState.toastMessage != null) {
                                LaunchedEffect(catalogState.toastMessage) {
                                    Toast.makeText(context, catalogState.toastMessage, Toast.LENGTH_SHORT).show()
                                    catalogViewModel.consumeToast()
                        }
                    }
                }

                UserRole.EMPLOYEE -> EmployeeScreen(modifier = Modifier.fillMaxSize())
                UserRole.MANAGER -> ManagerScreen(modifier = Modifier.fillMaxSize())
                null -> Unit
            }
        }
    }
}

@Composable
private fun SessionBar(
    email: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onLogout) {
            Text("Esci")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    SmartShopMobileTheme {
        // Preview mostra il composable di login
        LoginScreen(onLogin = { _, _, _ -> })
    }
}

private fun mapRole(user: User?): UserRole? {
    return UserRole.fromDbRole(user?.ruolo)
}
