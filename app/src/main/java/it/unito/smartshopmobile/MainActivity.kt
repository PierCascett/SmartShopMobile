/**
 * MainActivity.kt
 *
 * MVVM: Entry Point - Activity principale Android
 *
 * FUNZIONAMENTO:
 * - Activity host per tutta l'applicazione (Jetpack Compose)
 * - Inizializza ViewModel (by viewModels() - lifecycle-aware)
 * - Gestisce navigazione tra schermate in base a UserRole
 * - Applica tema Material Design globale
 * - Gestisce ciclo vita app (onCreate, configurazione changes)
 *
 * PATTERN MVVM:
 * - Controller Android: entry point app
 * - setContent: bridge da Activity a Compose
 * - viewModels(): crea ViewModel legato a lifecycle
 * - Navigation: routing Login → Customer/Employee/Manager
 * - State hoisting: stato sessione da SessionDataStore
 *
 * COMPONENTI:
 * - SmartShopApp: root Composable
 * - LoginScreenMVVM: schermata iniziale
 * - Routing condizionale: CatalogScreen / EmployeeScreen / ManagerScreen
 */
// Activity principale: avvio tema e schermata di login

package it.unito.smartshopmobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.domain.UserRole
import it.unito.smartshopmobile.ui.components.NavBarDivider
import it.unito.smartshopmobile.ui.screens.AccountSettingsScreen
import it.unito.smartshopmobile.ui.screens.AppCartOverlay
import it.unito.smartshopmobile.ui.screens.AppFavoritesOverlay
import it.unito.smartshopmobile.ui.screens.CatalogScreen
import it.unito.smartshopmobile.ui.screens.EmployeeScreen
import it.unito.smartshopmobile.ui.screens.EmployeeTab
import it.unito.smartshopmobile.ui.screens.LoginScreen
import it.unito.smartshopmobile.ui.screens.LoginScreenMVVM
import it.unito.smartshopmobile.ui.screens.ManagerScreen
import it.unito.smartshopmobile.ui.screens.ManagerTab
import it.unito.smartshopmobile.ui.screens.OrderHistoryPanel
import it.unito.smartshopmobile.ui.screens.SideMenuOverlay
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.AccountPreferencesViewModel
import it.unito.smartshopmobile.viewModel.CatalogUiState
import it.unito.smartshopmobile.viewModel.CatalogViewModel
import it.unito.smartshopmobile.viewModel.CustomerTab
import it.unito.smartshopmobile.viewModel.LoginViewModel
import it.unito.smartshopmobile.viewModel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Activity principale dell'applicazione SmartShop Mobile.
 *
 * Entry point dell'app che gestisce:
 * - Inizializzazione ViewModels con lifecycle-aware delegation
 * - Navigazione condizionale basata su UserRole (Customer/Employee/Manager)
 * - Applicazione del tema Material Design 3
 * - Gestione overlay (menu, carrello, preferiti)
 * - Sincronizzazione sessione tra ViewModels
 *
 * Architettura MVVM:
 * - Activity come Controller che coordina View (Compose) e ViewModels
 * - State hoisting: stato UI osservato tramite collectAsState()
 * - Unidirectional data flow: eventi → ViewModel → State → UI
 * - Separation of concerns: business logic nei ViewModels
 *
 * ViewModels gestiti:
 * - MainViewModel: stato globale app, sessione, navigazione
 * - LoginViewModel: autenticazione utente
 * - CatalogViewModel: catalogo prodotti, carrello, ordini (Customer)
 * - AccountPreferencesViewModel: preferenze profilo locale
 *
 * Routing basato su ruolo:
 * - null → LoginScreenMVVM
 * - CUSTOMER → CatalogScreen con bottom navigation
 * - EMPLOYEE → EmployeeScreen (mappa, picking ordini)
 * - MANAGER → ManagerScreen (inventario, riordini)
 *
 * @property mainViewModel ViewModel per stato globale e navigazione
 * @property loginViewModel ViewModel per login/registrazione
 * @property catalogViewModel ViewModel per catalogo e carrello
 * @property accountPreferencesViewModel ViewModel per preferenze locali
 */
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private val catalogViewModel: CatalogViewModel by viewModels()
    private val accountPreferencesViewModel: AccountPreferencesViewModel by viewModels()
    /**
     * On Create.
     */
    /**
     * On Create.
     */

    /**
     * Inizializza la MainActivity impostando tema, ViewModel e routing in base alla sessione.
     * Gestisce toast, sincronizzazione sessione con CatalogViewModel e overlay principali.
     */
    /**
     * Inizializza la MainActivity impostando tema, ViewModel e routing in base alla sessione.
     * Gestisce toast, sincronizzazione sessione con CatalogViewModel e overlay principali.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartShopMobileTheme {
                val context = LocalContext.current
                val mainState by mainViewModel.uiState.collectAsState()
                val catalogState by catalogViewModel.uiState.collectAsState()
                val accountPrefs by accountPreferencesViewModel.preferences.collectAsState()

                // Gestione Toast tramite LaunchedEffect (MVVM-compliant)
                LaunchedEffect(mainState.toastMessage) {
                    mainState.toastMessage?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        mainViewModel.consumeToast()
                    }
                }

                // Sincronizzazione session → catalogViewModel
                LaunchedEffect(mainState.loggedUser, mainState.selectedRole) {
                    mainState.loggedUser?.let { user ->
                        catalogViewModel.setLoggedUser(user)
                        if (mainState.selectedRole == UserRole.CUSTOMER) {
                            catalogViewModel.startSyncIfNeeded()
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0.dp)
                    ) { innerPadding ->
                        val contentModifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding()

                        if (mainState.loggedUser == null) {
                            LoginScreenMVVM(
                                viewModel = loginViewModel,
                                modifier = contentModifier,
                                onLoginSuccess = { user, role ->
                                    mainViewModel.onLoginSuccess(user, role)
                                })
                        } else {
                            ContentWithSessionBar(
                                modifier = contentModifier,
                                email = mainState.loggedUser?.email ?: "",
                                avatarUrl = catalogState.loggedUser?.avatarUrl,
                                role = mainState.selectedRole,
                                onLogout = {
                                    mainViewModel.onLogout()
                                    catalogViewModel.clearSession()
                                    loginViewModel.clearSession()
                                },
                                onOpenAccount = {
                                    mainViewModel.openAccountForRole(mainState.selectedRole)
                                },
                                catalogState = catalogState,
                                catalogViewModel = catalogViewModel,
                                employeeProfileTrigger = mainState.employeeProfileTrigger,
                                managerProfileTrigger = mainState.managerProfileTrigger,
                                onMenuClick = { mainViewModel.setShowMenu(true) },
                                onFavoritesClick = { mainViewModel.setShowFavorites(true) },
                                onCartClick = { mainViewModel.setShowCart(true) },
                                accountPrefs = accountPrefs,
                                onSaveProfile = { nome, cognome, email, indirizzo, telefono ->
                                    val result = catalogViewModel.updateUserProfile(
                                        nome, cognome, email, telefono.ifBlank { null })
                                    result.onSuccess {
                                        accountPreferencesViewModel.updateProfile(
                                            nome, cognome, indirizzo, telefono
                                        )
                                    }
                                    result
                                },
                                customerTab = mainState.currentTab,
                                onCustomerTabChange = { mainViewModel.setCurrentTab(it) })
                        }
                    }

                    if (mainState.showMenu) {
                        SideMenuOverlay(
                            onDismiss = { mainViewModel.setShowMenu(false) },
                            sections = catalogState.sideMenuSections,
                            onParentSelected = { parentId: String? ->
                                catalogViewModel.onParentCategorySelected(parentId)
                                catalogViewModel.onSearchQueryChange("")
                            },
                            onEntrySelected = { selection: String? ->
                                mainViewModel.setShowMenu(false)
                                catalogViewModel.onCategorySelected(selection)
                                catalogViewModel.onSearchQueryChange("")
                            })
                    }

                    if (mainState.showCart) {
                        AppCartOverlay(
                            onDismiss = {
                                mainViewModel.setShowCart(false)
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
                            onDeliveryMethodChange = { catalogViewModel.onDeliveryMethodSelected(it) })
                    }

                    if (mainState.showFavorites) {
                        AppFavoritesOverlay(
                            onDismiss = { mainViewModel.setShowFavorites(false) },
                            favorites = catalogState.favoriteProducts,
                            cartQuantities = catalogState.cart,
                            onIncrease = { catalogViewModel.onAddToCart(it) },
                            onDecrease = { catalogViewModel.onDecreaseCartItem(it) },
                            onToggleFavorite = { catalogViewModel.onBookmark(it) },
                            onProductClick = { catalogViewModel.onProductSelected(it) })
                    }

                }
            }
        }
    }
}
/**
 * Content With Session Bar.
 */
/**
 * Content With Session Bar.
 */

/**
 * Layout condiviso che avvolge i flussi Customer/Employee/Manager con barra sessione.
 * Applica TopAppBar con avatar/email e delega il contenuto al ruolo corrente.
 */
/**
 * Layout condiviso che avvolge i flussi Customer/Employee/Manager con barra sessione.
 * Applica TopAppBar con avatar/email e delega il contenuto al ruolo corrente.
 */
@Composable
private fun ContentWithSessionBar(
    modifier: Modifier,
    email: String,
    avatarUrl: String?,
    role: UserRole?,
    onLogout: () -> Unit,
    onOpenAccount: () -> Unit,
    catalogState: CatalogUiState,
    catalogViewModel: CatalogViewModel,
    employeeProfileTrigger: Int,
    managerProfileTrigger: Int,
    onMenuClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    accountPrefs: AccountPreferences,
    onSaveProfile: suspend (String, String, String, String, String) -> Result<User>,
    customerTab: CustomerTab,
    onCustomerTabChange: (CustomerTab) -> Unit
) {
    val resolvedEmail = catalogState.loggedUser?.email ?: email
    var employeeTab by rememberSaveable { mutableStateOf(EmployeeTab.PICKING) }
    var managerTab by rememberSaveable { mutableStateOf(ManagerTab.RESTOCK) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SessionBar(
                email = resolvedEmail,
                avatarUrl = avatarUrl,
                onLogout = onLogout,
                onOpenAccount = onOpenAccount
            )
        },
        bottomBar = {
            when (role) {
                UserRole.CUSTOMER -> CustomerBottomBar(
                    selectedTab = customerTab,
                    onTabChange = onCustomerTabChange
                )

                UserRole.EMPLOYEE -> EmployeeBottomBar(
                    selectedTab = employeeTab,
                    onTabChange = { employeeTab = it }
                )

                UserRole.MANAGER -> ManagerBottomBar(
                    selectedTab = managerTab,
                    onTabChange = { managerTab = it }
                )

                null -> Unit
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (role) {
                UserRole.CUSTOMER -> CustomerHome(
                    state = catalogState,
                    accountPrefs = accountPrefs,
                    catalogViewModel = catalogViewModel,
                    selectedTab = customerTab,
                    onTabChange = onCustomerTabChange,
                    onMenuClick = onMenuClick,
                    onFavoritesClick = onFavoritesClick,
                    onCartClick = onCartClick,
                    onSaveProfile = onSaveProfile
                )

                UserRole.EMPLOYEE -> EmployeeScreen(
                    modifier = Modifier.fillMaxSize(),
                    accountPrefs = accountPrefs,
                    loggedUserEmail = catalogState.loggedUser?.email.orEmpty(),
                    avatarUrl = catalogState.loggedUser?.avatarUrl,
                    openProfileTrigger = employeeProfileTrigger,
                    onSaveProfile = { nome, cognome, emailInput, indirizzo, telefono ->
                        catalogViewModel.updateUserProfile(
                            nome, cognome, emailInput, telefono.ifBlank { null })
                    },
                    onUploadPhoto = { uri -> catalogViewModel.uploadProfilePhoto(uri) },
                    selectedTab = employeeTab,
                    onTabChange = { employeeTab = it })

                UserRole.MANAGER -> ManagerScreen(
                    modifier = Modifier.fillMaxSize(),
                    accountPrefs = accountPrefs,
                    loggedUserEmail = catalogState.loggedUser?.email.orEmpty(),
                    avatarUrl = catalogState.loggedUser?.avatarUrl,
                    openProfileTrigger = managerProfileTrigger,
                    onSaveProfile = { nome, cognome, emailInput, indirizzo, telefono ->
                        catalogViewModel.updateUserProfile(
                            nome, cognome, emailInput, telefono.ifBlank { null })
                    },
                    onUploadPhoto = { uri -> catalogViewModel.uploadProfilePhoto(uri) },
                    selectedTab = managerTab,
                    onTabChange = { managerTab = it })

                null -> Unit
            }
        }
    }
}
/**
 * Customer Home.
 */
/**
 * Customer Home.
 */

/**
 * Contenuto principale per il ruolo Customer con bottom navigation (Shop/Orders/Account).
 * Coordina snackbar, tab correnti e delega alle schermate Catalog, Storico e Account.
 */
/**
 * Contenuto principale per il ruolo Customer con bottom navigation (Shop/Orders/Account).
 * Coordina snackbar, tab correnti e delega alle schermate Catalog, Storico e Account.
 */
@Composable
private fun CustomerHome(
    state: CatalogUiState,
    accountPrefs: AccountPreferences,
    catalogViewModel: CatalogViewModel,
    selectedTab: CustomerTab,
    onTabChange: (CustomerTab) -> Unit,
    onMenuClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    onSaveProfile: suspend (String, String, String, String, String) -> Result<it.unito.smartshopmobile.data.entity.User>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var savingProfile by rememberSaveable { mutableStateOf(false) }
    var uploadingPhoto by rememberSaveable { mutableStateOf(false) }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileSuccess by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(selectedTab) {
        if (selectedTab == CustomerTab.ORDERS) {
            catalogViewModel.refreshOrderHistory()
        }
    }

    val resolvedPrefs = accountPrefs.copy(
        nome = accountPrefs.nome.ifBlank { state.loggedUser?.nome.orEmpty() },
        cognome = accountPrefs.cognome.ifBlank { state.loggedUser?.cognome.orEmpty() },
        telefono = accountPrefs.telefono.ifBlank { state.loggedUser?.telefono.orEmpty() })

    LaunchedEffect(selectedTab, state.errorMessage) {
        val shouldRetryCatalog = selectedTab == CustomerTab.SHOP && state.errorMessage != null
        if (shouldRetryCatalog) {
            catalogViewModel.refreshCatalog()
        }
    }

    LaunchedEffect(resolvedPrefs.indirizzoSpedizione, resolvedPrefs.telefono) {
        catalogViewModel.setCustomerContacts(
            indirizzo = resolvedPrefs.indirizzoSpedizione,
            telefono = resolvedPrefs.telefono.ifBlank { null })
    }

    LaunchedEffect(profileError) {
        profileError?.let {
            snackbarHostState.showSnackbar(it)
            profileError = null
        }
    }
    LaunchedEffect(profileSuccess) {
        profileSuccess?.let {
            snackbarHostState.showSnackbar(it)
            profileSuccess = null
        }
    }
    LaunchedEffect(state.toastMessage, state.showToast) {
        if (state.showToast && state.toastMessage != null) {
            snackbarHostState.showSnackbar(state.toastMessage!!)
            catalogViewModel.consumeToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                onFavoritesClick = onFavoritesClick,
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
                snackbarHostState = snackbarHostState,
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            CustomerTab.ACCOUNT -> AccountSettingsScreen(
                preferences = resolvedPrefs,
                email = state.loggedUser?.email.orEmpty(),
                avatarUrl = state.loggedUser?.avatarUrl,
                isSaving = savingProfile,
                isUploadingPhoto = uploadingPhoto,
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
                onPickNewPhoto = { uri ->
                    uploadingPhoto = true
                    profileError = null
                    profileSuccess = null
                    scope.launch {
                        val uploadResult = catalogViewModel.uploadProfilePhoto(uri)
                        uploadResult.onSuccess {
                            profileSuccess = "Foto profilo aggiornata"
                        }.onFailure { error ->
                            profileError = error.message
                        }
                        uploadingPhoto = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
    }
}
/**
 * Session Bar.
 */
/**
 * Session Bar.
 */

/**
 * Barra superiore della sessione con avatar/email e pulsante logout.
 * Mostra l'avatar da backend (cache-busted) o un'icona di fallback.
 */
/**
 * Barra superiore della sessione con avatar/email e pulsante logout.
 * Mostra l'avatar da backend (cache-busted) o un'icona di fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionBar(
    email: String,
    avatarUrl: String?,
    onLogout: () -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarModel = remember(avatarUrl) {
        avatarUrl?.let { url ->
            RetrofitInstance.buildAssetUrl(url)
                ?.let { built -> "$built?ts=${System.currentTimeMillis()}" }
        }
    }
    TopAppBar(
        modifier = modifier, title = {
        Text(
            text = email.ifBlank { "Account" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onOpenAccount() })
    }, navigationIcon = {
        if (!avatarModel.isNullOrBlank()) {
            AsyncImage(
                model = avatarModel,
                contentDescription = "Avatar",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onOpenAccount() })
        } else {
            IconButton(onClick = onOpenAccount) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle, contentDescription = "Account"
                )
            }
        }
    }, actions = {
        TextButton(onClick = onLogout) {
            Text("Esci")
        }
    }, colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.primary
    ), windowInsets = WindowInsets(0.dp)
    )
}

/**
 * Bottom bar per il ruolo Customer.
 */
@Composable
private fun CustomerBottomBar(selectedTab: CustomerTab, onTabChange: (CustomerTab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NavBarDivider()
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = WindowInsets(0.dp)
        ) {
            NavigationBarItem(
                selected = selectedTab == CustomerTab.SHOP,
                onClick = { onTabChange(CustomerTab.SHOP) },
                icon = { Icon(Icons.Filled.Home, contentDescription = "Catalogo") },
                label = { Text("Ordina") })
            NavigationBarItem(
                selected = selectedTab == CustomerTab.ORDERS,
                onClick = { onTabChange(CustomerTab.ORDERS) },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.List, contentDescription = "Ordini"
                    )
                },
                label = { Text("Storico") })
            NavigationBarItem(
                selected = selectedTab == CustomerTab.ACCOUNT,
                onClick = { onTabChange(CustomerTab.ACCOUNT) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Account") },
                label = { Text("Account") })
        }
    }
}

/**
 * Bottom bar per il ruolo Employee.
 */
@Composable
private fun EmployeeBottomBar(selectedTab: EmployeeTab, onTabChange: (EmployeeTab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NavBarDivider()
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = WindowInsets(0.dp)
        ) {
            EmployeeTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selectedTab,
                    onClick = { onTabChange(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }
}

/**
 * Bottom bar per il ruolo Manager.
 */
@Composable
private fun ManagerBottomBar(selectedTab: ManagerTab, onTabChange: (ManagerTab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NavBarDivider()
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = WindowInsets(0.dp)
        ) {
            ManagerTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selectedTab,
                    onClick = { onTabChange(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }
}

/**
 * Composable per gestire main preview.
 */
@Preview(showBackground = true)
@Composable
fun MainPreview() {
    SmartShopMobileTheme {
        // Preview mostra il composable di login
        LoginScreen(onLogin = { _, _, _ -> })
    }
}
/**
 * Map Role.
 */
/**
 * Map Role.
 */

/** Converte il ruolo stringa del backend in enum `UserRole`. */
/** Converte il ruolo stringa del backend in enum `UserRole`. */
private fun mapRole(user: User?): UserRole? {
    return UserRole.fromDbRole(user?.ruolo)
}
