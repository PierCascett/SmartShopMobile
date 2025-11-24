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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.model.UserRole
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.ui.screens.CatalogScreen
import it.unito.smartshopmobile.ui.screens.LoginScreenMVVM
import it.unito.smartshopmobile.ui.screens.LoginScreen
import it.unito.smartshopmobile.ui.screens.EmployeeScreen
import it.unito.smartshopmobile.ui.screens.ManagerScreen
import it.unito.smartshopmobile.ui.screens.SideMenuOverlay
import it.unito.smartshopmobile.ui.screens.AppCartOverlay
import it.unito.smartshopmobile.ui.screens.OrderHistoryPanel
import it.unito.smartshopmobile.ui.screens.AccountSettingsScreen
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.AccountPreferencesViewModel
import it.unito.smartshopmobile.viewModel.CatalogViewModel
import it.unito.smartshopmobile.viewModel.CatalogUiState
import it.unito.smartshopmobile.viewModel.LoginViewModel

import it.unito.smartshopmobile.ui.components.NavBarDivider
import kotlinx.coroutines.launch
private enum class CustomerTab { SHOP, ORDERS, ACCOUNT }

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val catalogViewModel: CatalogViewModel by viewModels()
    private val accountPreferencesViewModel: AccountPreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartShopMobileTheme {
                var loggedUser by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
                var showMenu by rememberSaveable { mutableStateOf(false) }
                var showCart by rememberSaveable { mutableStateOf(false) }
                var customerTab by rememberSaveable { mutableStateOf(CustomerTab.SHOP) }
                val catalogState by catalogViewModel.uiState.collectAsState()
                val sessionUser by loginViewModel.sessionUser.collectAsState(initial = null)
                val accountPrefs by accountPreferencesViewModel.preferences.collectAsState()
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
                                    customerTab = CustomerTab.SHOP
                                    catalogViewModel.clearSession()
                                    loginViewModel.clearSession()
                                },
                                catalogState = catalogState,
                                catalogViewModel = catalogViewModel,
                                onMenuClick = { showMenu = true },
                                onCartClick = { showCart = true },
                                accountPrefs = accountPrefs,
                                onSaveProfile = { nome: String, cognome: String, email: String, indirizzo: String, telefono: String ->
                                    val result = catalogViewModel.updateCustomerProfile(nome, cognome, email, telefono.ifBlank { null })
                                    result.onSuccess {
                                        accountPreferencesViewModel.updateProfile(nome, cognome, indirizzo, telefono)
                                    }
                                    result
                                },
                                customerTab = customerTab,
                                onCustomerTabChange = { customerTab = it }
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
    accountPrefs: AccountPreferences,
    onSaveProfile: suspend (String, String, String, String, String) -> Result<User>,
    customerTab: CustomerTab,
    onCustomerTabChange: (CustomerTab) -> Unit
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
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            when (role) {
                UserRole.CUSTOMER -> CustomerHome(
                    state = catalogState,
                    accountPrefs = accountPrefs,
                    catalogViewModel = catalogViewModel,
                    selectedTab = customerTab,
                    onTabChange = onCustomerTabChange,
                    onMenuClick = onMenuClick,
                    onCartClick = onCartClick,
                    onSaveProfile = onSaveProfile
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
private fun CustomerHome(
    state: CatalogUiState,
    accountPrefs: AccountPreferences,
    catalogViewModel: CatalogViewModel,
    selectedTab: CustomerTab,
    onTabChange: (CustomerTab) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onSaveProfile: suspend (String, String, String, String, String) -> Result<it.unito.smartshopmobile.data.entity.User>
) {
    val scope = rememberCoroutineScope()
    var savingProfile by rememberSaveable { mutableStateOf(false) }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileSuccess by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedTab) {
        if (selectedTab == CustomerTab.ORDERS) {
            catalogViewModel.refreshOrderHistory()
        }
    }

    val resolvedPrefs = accountPrefs.copy(
        nome = accountPrefs.nome.ifBlank { state.loggedUser?.nome.orEmpty() },
        cognome = accountPrefs.cognome.ifBlank { state.loggedUser?.cognome.orEmpty() },
        telefono = accountPrefs.telefono.ifBlank { state.loggedUser?.telefono.orEmpty() }
    )

    LaunchedEffect(resolvedPrefs.indirizzoSpedizione, resolvedPrefs.telefono) {
        catalogViewModel.setCustomerContacts(
            indirizzo = resolvedPrefs.indirizzoSpedizione,
            telefono = resolvedPrefs.telefono.ifBlank { null }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                NavBarDivider()
                NavigationBar(
                     modifier = Modifier.fillMaxWidth(),
                     containerColor = MaterialTheme.colorScheme.surface,
                     contentColor = MaterialTheme.colorScheme.onSurface,
                     windowInsets = WindowInsets(0.dp)
                ) {
                     NavigationBarItem(
                         selected = selectedTab == CustomerTab.SHOP,
                         onClick = { onTabChange(CustomerTab.SHOP) },
                         icon = { Icon(Icons.Filled.Home, contentDescription = "Catalogo") },
                         label = { Text("Ordina") }
                     )
                     NavigationBarItem(
                         selected = selectedTab == CustomerTab.ORDERS,
                         onClick = { onTabChange(CustomerTab.ORDERS) },
                         icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ordini") },
                         label = { Text("Storico") }
                     )
                     NavigationBarItem(
                         selected = selectedTab == CustomerTab.ACCOUNT,
                         onClick = { onTabChange(CustomerTab.ACCOUNT) },
                         icon = { Icon(Icons.Filled.Settings, contentDescription = "Account") },
                         label = { Text("Account") }
                     )
                 }
             }
         }
     ) { innerPadding ->
         val contentPadding = PaddingValues(
             start = innerPadding.calculateLeftPadding(LayoutDirection.Ltr),
             end = innerPadding.calculateRightPadding(LayoutDirection.Ltr),
             top = innerPadding.calculateTopPadding(),
             bottom = innerPadding.calculateBottomPadding()
         )
        when (selectedTab) {
            CustomerTab.SHOP -> CatalogScreen(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                onMenuClick = onMenuClick,
                onCartClick = onCartClick,
                onHistoryClick = {
                    onTabChange(CustomerTab.ORDERS)
                    catalogViewModel.refreshOrderHistory()
                },
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
            )

            CustomerTab.ORDERS -> OrderHistoryPanel(
                orders = state.orderHistory,
                isLoading = state.isOrderHistoryLoading,
                error = state.orderHistoryError,
                pickupInProgressId = state.pickupInProgressId,
                pickupMessage = state.pickupMessage,
                onRefresh = { catalogViewModel.refreshOrderHistory() },
                onScanQr = { catalogViewModel.simulateLockerPickup(it) },
                onDismissMessage = catalogViewModel::clearPickupMessage,
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            CustomerTab.ACCOUNT -> AccountSettingsScreen(
                preferences = resolvedPrefs,
                email = state.loggedUser?.email.orEmpty(),
                isSaving = savingProfile,
                error = profileError,
                success = profileSuccess,
                onSaveProfile = { nome, cognome, emailInput, indirizzo, telefono ->
                    savingProfile = true
                    profileError = null
                    profileSuccess = null
                    scope.launch {
                        val result: Result<it.unito.smartshopmobile.data.entity.User> =
                            onSaveProfile(nome, cognome, emailInput, indirizzo, telefono)
                        result.onSuccess { _ ->
                            profileSuccess = "Dati aggiornati"
                        }.onFailure { error ->
                            profileError = error.message
                        }
                        savingProfile = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
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
