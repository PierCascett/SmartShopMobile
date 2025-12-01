/**
 * ManagerScreen.kt
 *
 * MVVM: View Layer - UI per schermata Responsabile
 *
 * FUNZIONAMENTO:
 * - Osserva ManagerViewModel.uiState per gestione magazzino
 * - Form riordino merce da fornitori (Restock)
 * - Trasferimento stock da magazzino a scaffali
 * - Visualizza storico riordini e arrivi
 * - Tabs: Riordine, Storico, Trasferisci, Profilo
 *
 * PATTERN MVVM:
 * - View: solo rendering UI dashboard amministrativa
 * - collectAsState(): osserva StateFlow<ManagerUiState>
 * - Intent al ViewModel: submitRestock(), moveStockToShelf(), refreshAllData()
 * - Unidirectional Data Flow: UI event → ViewModel → State → UI
 * - Componenti: RestockForm, RestockList, TransferForm, ProductSelector
 */
package it.unito.smartshopmobile.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.unito.smartshopmobile.ui.components.NavBarDivider
import it.unito.smartshopmobile.viewModel.ManagerUiState
import it.unito.smartshopmobile.viewModel.ManagerViewModel
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.User
import java.util.Comparator
import java.util.Locale
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.unito.smartshopmobile.ui.components.StoreMapCanvas
import it.unito.smartshopmobile.ui.map.rememberAssetImage
import kotlinx.coroutines.launch

private enum class ManagerTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RESTOCK("Riordine", Icons.AutoMirrored.Filled.Assignment),
    LIST("Storico", Icons.AutoMirrored.Filled.List),
    TRANSFER("Trasferisci", Icons.Filled.Refresh),
    PROFILE("Profilo", Icons.Filled.Settings)
}

private enum class RestockFilter { INBOUND, ARRIVED }

@Composable
fun ManagerScreen(
    modifier: Modifier = Modifier,
    accountPrefs: AccountPreferences = AccountPreferences(),
    loggedUserEmail: String = "",
    avatarUrl: String? = null,
    openProfileTrigger: Int = 0,
    onSaveProfile: suspend (String, String, String, String, String) -> Result<User> = { _, _, _, _, _ -> Result.failure(Exception("Non configurato")) },
    onUploadPhoto: suspend (Uri) -> Result<String> = { Result.failure(Exception("Non configurato")) },
    viewModel: ManagerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingProfile by rememberSaveable { mutableStateOf(false) }
    var uploadingPhoto by rememberSaveable { mutableStateOf(false) }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileSuccess by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(state.transferSuccess) {
        state.transferSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearTransferSuccess()
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(ManagerTab.RESTOCK) }
    LaunchedEffect(openProfileTrigger) {
        if (openProfileTrigger > 0) {
            selectedTab = ManagerTab.PROFILE
        }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                NavBarDivider()
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    ManagerTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val basePadding = Modifier.padding(
            start = innerPadding.calculateLeftPadding(LayoutDirection.Ltr) + 16.dp,
            end = innerPadding.calculateRightPadding(LayoutDirection.Ltr) + 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding()
        )
        when (selectedTab) {
            ManagerTab.RESTOCK -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(basePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RestockForm(
                        state = state,
                        onCategorySelected = viewModel::onCategorySelected,
                        onProductSelected = viewModel::onProductSelected,
                        onSupplierSelected = viewModel::onSupplierSelected,
                        onQuantityChange = viewModel::onQuantityChanged,
                        onShowProduct = { viewModel.showProductDetail(it) },
                        onRefresh = viewModel::refreshAllData,
                        onSubmit = { viewModel.submitRestock() }
                    )
                }
            }
            ManagerTab.TRANSFER -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(basePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TransferForm(
                        state = state,
                        onCategorySelected = viewModel::onCategorySelected,
                    onProductSelected = viewModel::onProductSelected,
                    onShowProduct = { viewModel.showProductDetail(it) },
                    onShelfSelected = viewModel::onShelfSelected,
                    onQuantityChange = viewModel::onTransferQuantityChanged,
                    onSubmit = viewModel::moveStockToShelf,
                    onRefresh = viewModel::refreshAllData
                )
                }
            }
            ManagerTab.LIST -> RestockList(
                state = state,
                onRefresh = viewModel::refreshRestocks,
                onShowProduct = { viewModel.showProductDetail(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .then(basePadding)
            )
            ManagerTab.PROFILE -> {
                AccountSettingsScreen(
                    preferences = accountPrefs,
                    email = loggedUserEmail,
                    avatarUrl = avatarUrl,
                    isSaving = savingProfile,
                    isUploadingPhoto = uploadingPhoto,
                    onSaveProfile = { nome, cognome, email, indirizzo, telefono ->
                        savingProfile = true
                        profileError = null
                        profileSuccess = null
                        scope.launch {
                            val result = onSaveProfile(nome, cognome, email, indirizzo, telefono)
                            result.onSuccess { profileSuccess = "Dati aggiornati" }
                                .onFailure { profileError = it.message }
                            savingProfile = false
                        }
                    },
                    onPickNewPhoto = { uri ->
                        uploadingPhoto = true
                        profileError = null
                        profileSuccess = null
                        scope.launch {
                            val uploadResult = onUploadPhoto(uri)
                            uploadResult.onSuccess { profileSuccess = "Foto aggiornata" }
                                .onFailure { profileError = it.message }
                            uploadingPhoto = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(basePadding)
                )
            }
        }
    }

    state.showProductDetail?.let { product ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissProductDetail() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissProductDetail() }) {
                    Text("Chiudi")
                }
            },
            title = { Text(product.name, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Marca: ${product.brand}")
                    Text("Categoria: ${product.categoryName ?: product.categoryId}")
                    Text("Prezzo:  ${String.format(Locale.ROOT, "%.2f", product.price)}")
                    product.description?.takeIf { it.isNotBlank() }?.let { desc -> Text(desc) }
                }
            }
        )
    }
}

@Composable
private fun RestockForm(
    state: ManagerUiState,
    onCategorySelected: (String?) -> Unit,
    onProductSelected: (String) -> Unit,
    onSupplierSelected: (Int) -> Unit,
    onQuantityChange: (String) -> Unit,
    onShowProduct: (String) -> Unit,
    onRefresh: () -> Unit,
    onSubmit: () -> Unit
) {
    var productQuery by rememberSaveable { mutableStateOf("") }
    var showAllProducts by rememberSaveable { mutableStateOf(false) }
    var showCategoryModal by rememberSaveable { mutableStateOf(false) }
    var showSupplierModal by rememberSaveable { mutableStateOf(false) }
    val sideMenuSections = remember(state.categories) { buildSideMenuSectionsFromCategories(state.categories) }
    val filteredProducts = state.availableProducts
        .filter { product ->
            val query = productQuery.trim()
            val matchesQuery = if (query.isBlank()) true else {
                product.name.contains(query, ignoreCase = true) ||
                    product.brand.contains(query, ignoreCase = true) ||
                    (product.description?.contains(query, ignoreCase = true) == true)
            }
            val matchesCategory = if (query.isNotBlank()) true else state.selectedCategoryId?.let { product.categoryId == it } ?: true
            matchesQuery && matchesCategory
        }
    val selectedProduct = state.availableProducts.firstOrNull { it.id == state.selectedProductId }
    val selectedSupplier = state.suppliers.firstOrNull { it.id == state.selectedSupplierId }
    LaunchedEffect(Unit) {
        if (!state.selectedProductId.isNullOrBlank()) {
            onProductSelected("")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showCategoryModal = true },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Categorie")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading,
                    modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna dati", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aggiorna")
                }
            }
            ProductPickerHeader(
                query = productQuery,
                onQueryChange = {
                    productQuery = it
                    onCategorySelected(null)
                    onProductSelected("") // reset selezione su nuova ricerca
                },
                onClearQuery = {
                    productQuery = ""
                    onCategorySelected(null)
                    onProductSelected("")
                },
                onOpenCategories = { showCategoryModal = true },
                showCategoriesButton = false
            )

        ProductCompactList(
            products = filteredProducts,
            selectedProductId = state.selectedProductId,
            onSelect = onProductSelected,
            onShowAllToggle = { showAllProducts = !showAllProducts },
            showAll = showAllProducts,
            onShowProduct = onShowProduct,
            priceBuilder = { product: it.unito.smartshopmobile.data.entity.Product -> "€ ${String.format(Locale.ROOT, "%.2f", product.price)}" },
            showStockRow = false,
            selectedContainerColor = colorScheme.surfaceVariant,
            unselectedContainerColor = colorScheme.surface
        )
            if (filteredProducts.isNotEmpty() && state.selectedProductId.isNullOrBlank()) {
                Text("Seleziona un prodotto da riordinare", color = colorScheme.onSurfaceVariant)
            }

            if (state.suppliers.isEmpty()) {
                Text("Nessun fornitore disponibile", color = colorScheme.onSurfaceVariant)
            } else {
                TextButton(
                    onClick = { showSupplierModal = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedSupplier?.name ?: "Fornitori")
                }
            }

        OutlinedTextField(
            value = state.quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantita") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Arrivo previsto automatico in circa 30 secondi", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Invia riordino")
        }
            state.error?.let { Text(it, color = colorScheme.error) }
            state.successMessage?.let { Text(it, color = colorScheme.primary) }
        }
    }

    if (showCategoryModal) {
        Dialog(
            onDismissRequest = { showCategoryModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SideMenuOverlay(
                onDismiss = { showCategoryModal = false },
                sections = sideMenuSections,
                onParentSelected = { parent -> onCategorySelected(parent) },
                onEntrySelected = { id ->
                    onCategorySelected(id)
                    onProductSelected("")
                    showCategoryModal = false
                }
            )
        }
    }

    if (showSupplierModal) {
        SupplierPickerDialog(
            suppliers = state.suppliers,
            selectedId = state.selectedSupplierId,
            onSelect = {
                onSupplierSelected(it)
                showSupplierModal = false
            },
            onDismiss = { showSupplierModal = false }
        )
    }
}

@Composable
private fun TransferForm(
    state: ManagerUiState,
    onCategorySelected: (String?) -> Unit,
    onProductSelected: (String) -> Unit,
    onShowProduct: (String) -> Unit,
    onShelfSelected: (Int) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRefresh: () -> Unit
) {
    var productQuery by rememberSaveable { mutableStateOf("") }
    var showCategoryModal by rememberSaveable { mutableStateOf(false) }
    var showAllProducts by rememberSaveable { mutableStateOf(false) }
    val selectedProduct = state.availableProducts.firstOrNull { it.id == state.selectedProductId }
    val filteredProducts = state.availableProducts
        .filter { product ->
            val query = productQuery.trim()
            val matchesQuery = if (query.isBlank()) true else {
                product.name.contains(query, true) || product.brand.contains(query, true)
            }
            val matchesCategory = if (query.isNotBlank()) true else state.selectedCategoryId?.let { product.categoryId == it } ?: true
            matchesQuery && matchesCategory
    }
        .sortedWith(
            compareByDescending<it.unito.smartshopmobile.data.entity.Product> { it.warehouseQuantity }
                .thenBy { it.name }
        )
    val mapImage = rememberAssetImage("map/supermarket_resized.png")
    val sideMenuSections = remember(state.categories) { buildSideMenuSectionsFromCategories(state.categories) }
    LaunchedEffect(Unit) {
        if (!state.selectedProductId.isNullOrBlank()) {
            onProductSelected("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { showCategoryModal = true },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Menu, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Categorie")
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna dati", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiorna")
            }
        }

        ProductPickerHeader(
            query = productQuery,
            onQueryChange = {
                productQuery = it
                onProductSelected("") // reset selezione su nuova ricerca
                onCategorySelected(null)
            },
            onClearQuery = {
                productQuery = ""
                onCategorySelected(null)
                onProductSelected("")
            },
            onOpenCategories = { showCategoryModal = true },
            showCategoriesButton = false
        )

        ProductCompactList(
            products = filteredProducts,
            selectedProductId = state.selectedProductId,
            onSelect = onProductSelected,
            onShowAllToggle = { showAllProducts = !showAllProducts },
            showAll = showAllProducts,
            onShowProduct = onShowProduct,
            priceBuilder = { product: it.unito.smartshopmobile.data.entity.Product -> "€ ${String.format(Locale.ROOT, "%.2f", product.price)}" },
            showStockRow = false,
            selectedContainerColor = colorScheme.surfaceVariant,
            unselectedContainerColor = colorScheme.surface
        )
        if (filteredProducts.isNotEmpty() && state.selectedProductId.isNullOrBlank()) {
            Text("Seleziona un prodotto per il trasferimento", color = colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 260.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            StoreMapCanvas(
                selectedAisleId = state.selectedShelfId?.toString(),
                onAisleClick = { id -> id.toIntOrNull()?.let(onShelfSelected) },
                background = mapImage,
                modifier = Modifier.fillMaxSize()
            )
        }

        OutlinedTextField(
            value = state.transferQuantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantita da spostare") },
            modifier = Modifier.fillMaxWidth()
        )
        QuickQuantityRow(currentValue = state.transferQuantity, onQuantityChange = onQuantityChange, options = listOf(1, 5, 10, -10, -5, -1))
        Button(
            onClick = onSubmit,
            enabled = !state.isTransferring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isTransferring) "Trasferimento..." else "Trasferisci scorte")
        }
        state.transferError?.let { Text(it, color = colorScheme.error) }
        state.transferSuccess?.let { Text(it, color = colorScheme.primary) }
    }

    if (showCategoryModal) {
        Dialog(
            onDismissRequest = { showCategoryModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SideMenuOverlay(
                onDismiss = { showCategoryModal = false },
                sections = sideMenuSections,
                onParentSelected = { parent -> onCategorySelected(parent) },
                onEntrySelected = { id ->
                    onCategorySelected(id)
                    onProductSelected("")
                    showCategoryModal = false
                }
            )
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = colorScheme.primary.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = colorScheme.primary)
    }
}

@Composable
private fun ProductPickerHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenCategories: () -> Unit,
    showCategoriesButton: Boolean = true
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Cerca") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClearQuery) { Icon(Icons.Filled.Close, contentDescription = "Pulisci ricerca") }
                }
            }
        )
        if (showCategoriesButton) {
            TextButton(
                onClick = onOpenCategories,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Filled.Menu, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Categorie")
            }
        }
    }
}

@Composable
private fun ProductCompactList(
    products: List<it.unito.smartshopmobile.data.entity.Product>,
    selectedProductId: String?,
    onSelect: (String) -> Unit,
    onShowAllToggle: () -> Unit,
    showAll: Boolean,
    onShowProduct: (String) -> Unit,
    priceBuilder: ((it.unito.smartshopmobile.data.entity.Product) -> String)?,
    showStockRow: Boolean = true,
    selectedContainerColor: androidx.compose.ui.graphics.Color = colorScheme.surface,
    unselectedContainerColor: androidx.compose.ui.graphics.Color = colorScheme.surfaceVariant
) {
    if (products.isEmpty()) {
        Text("Nessun prodotto per questi filtri", color = colorScheme.onSurfaceVariant)
        return
    }
    val visible = if (showAll) products else products.take(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visible.forEach { product ->
            val selected = product.id == selectedProductId
            val showQuantities = showStockRow || selected
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(product.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) selectedContainerColor else unselectedContainerColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(product.brand, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                        priceBuilder?.let { builder ->
                            SummaryChip(builder(product))
                        }
                    }
                    if (showQuantities) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SummaryChip("Magazzino ${product.warehouseQuantity}")
                            SummaryChip("Catalogo ${product.catalogQuantity}")
                        }
                    }
                }
            }
        }
        if (products.size > 2) {
            TextButton(onClick = onShowAllToggle, modifier = Modifier.align(Alignment.End)) {
                Text(if (showAll) "Mostra meno" else "Visualizza tutti")
            }
        }
    }
}

@Composable
private fun CategoryProductDialog(
    sections: List<CategorySectionUi>,
    products: List<it.unito.smartshopmobile.data.entity.Product>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onSelectProduct: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categorie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onDismiss) { Text("Chiudi") }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        TextButton(
                            onClick = { onSelectCategory(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surface)
                                .padding(vertical = 2.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Tutte le categorie", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                    sections.forEach { section ->
                        item {
                            Text(section.title ?: "Categorie", fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                        }
                        items(section.children) { category ->
                            val selected = category.id == selectedCategoryId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colorScheme.surfaceVariant),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = { onSelectCategory(if (selected) null else category.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        category.name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = colorScheme.onSurface
                                    )
                                }
                                if (selected) {
                                    val productsForCategory = products.filter { it.categoryId == category.id }
                                    if (productsForCategory.isEmpty()) {
                                        Text(
                                            "Nessun prodotto",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            productsForCategory.forEach { product ->
                                                TextButton(
                                                    onClick = { onSelectProduct(product.id) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                                ) {
                                                    Text(product.name, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CategorySectionUi(
    val id: String,
    val title: String?,
    val children: List<CategoryChildUi>
)

private data class CategoryChildUi(
    val id: String,
    val name: String
)

@Composable
private fun SupplierPickerDialog(
    suppliers: List<it.unito.smartshopmobile.data.entity.Supplier>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scegli fornitore", fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onDismiss) { Text("Chiudi") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(suppliers) { supplier ->
                        val selected = supplier.id == selectedId
                        TextButton(
                            onClick = { onSelect(supplier.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(supplier.name, fontWeight = FontWeight.SemiBold, color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface)
                                supplier.phone?.let { Text("Tel: $it", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant) }
                                supplier.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant) }
                                supplier.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductTransferGrid(
    products: List<it.unito.smartshopmobile.data.entity.Product>,
    selectedProductId: String?,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(products, key = { it.catalogId }) { product ->
            val selected = product.id == selectedProductId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(product.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) colorScheme.surface else colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(product.brand, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                        SummaryChip("${product.warehouseQuantity} in magazzino")
                    }
                    Text(
                        "Catalogo ${product.catalogQuantity} | Totale ${product.totalQuantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickQuantityRow(
    currentValue: String,
    onQuantityChange: (String) -> Unit,
    options: List<Int> = listOf(1, 5, 10, -10, -5, -1)
) {
    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val base = currentValue.toIntOrNull() ?: 0
        options.forEach { inc ->
            val label = if (inc > 0) "+$inc" else inc.toString()
            AssistChip(
                onClick = {
                    val newValue = (base + inc).coerceAtLeast(0)
                    onQuantityChange(newValue.toString())
                },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun RestockList(
    state: ManagerUiState,
    onRefresh: () -> Unit,
    onShowProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(RestockFilter.INBOUND) }
    val incoming = state.restocks.filter { !it.arrivato }
    val arrived = state.restocks.filter { it.arrivato }
    val filtered = when (selectedFilter) {
        RestockFilter.INBOUND -> incoming
        RestockFilter.ARRIVED -> arrived
    }
    val pageSize = 3 // card più alte: meno elementi per pagina per mantenere leggibilità
    var currentPage by rememberSaveable { mutableStateOf(0) }
    val pageCount = ((filtered.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    currentPage = currentPage.coerceIn(0, pageCount - 1)
    val pageItems = filtered.drop(currentPage * pageSize).take(pageSize)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RestockFilterChips(
                selected = selectedFilter,
                onSelect = {
                    selectedFilter = it
                    currentPage = 0
                },
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna storico", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiorna")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("In arrivo: ${incoming.size}", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
            Text("Arrivati: ${arrived.size}", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { Text(it, color = colorScheme.error) }

        when {
            pageItems.isEmpty() -> {
                Text("Nessun riordino per questo filtro", color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    items(pageItems, key = { it.idRiordino }) { restock ->
                        RestockHistoryCard(restock = restock, onShowProduct = onShowProduct)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pagina ${currentPage + 1} / $pageCount", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = { if (currentPage > 0) currentPage -= 1 },
                    enabled = currentPage > 0
                ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Pagina precedente") }
                FilledTonalIconButton(
                    onClick = { if (currentPage < pageCount - 1) currentPage += 1 },
                    enabled = currentPage < pageCount - 1
                ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Pagina successiva") }
            }
        }
    }
}

@Composable
private fun RestockHistoryCard(restock: it.unito.smartshopmobile.data.entity.Restock, onShowProduct: (String) -> Unit) {
    val statusLabel = if (restock.arrivato) "Arrivato" else "In arrivo"
    val statusColor = if (restock.arrivato) colorScheme.primary else colorScheme.secondary
    val subtleColor = colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Riordino #${restock.idRiordino}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    restock.dataOrdine.takeIf { it.isNotBlank() }?.let {
                        Text("Ordine: ${formatRestockDate(it)}", style = MaterialTheme.typography.bodySmall, color = subtleColor)
                    }
                }
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(restock.prodottoNome, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Fornitore: ${restock.fornitoreNome}", style = MaterialTheme.typography.bodySmall, color = subtleColor)
            Text("Quantita ordinata: ${restock.quantitaOrdinata}", style = MaterialTheme.typography.bodySmall)
            val arrivalLabel = when {
                restock.dataArrivoEffettiva != null -> "Arrivato il ${formatRestockDate(restock.dataArrivoEffettiva)}"
                restock.dataArrivoPrevista != null -> "Previsto: ${formatRestockDate(restock.dataArrivoPrevista)}"
                else -> "Data arrivo non disponibile"
            }
            Text(arrivalLabel, style = MaterialTheme.typography.bodySmall, color = subtleColor)
            restock.responsabileNome?.let { name ->
                Text(
                    "Responsabile: $name ${restock.responsabileCognome.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleColor
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onShowProduct(restock.idProdotto) }) {
                    Text("Dettagli prodotto")
                }
            }
        }
    }
}

@Composable
private fun RestockFilterChips(selected: RestockFilter, onSelect: (RestockFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        RestockFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            AssistChip(
                onClick = { onSelect(filter) },
                label = { Text(if (filter == RestockFilter.INBOUND) "In arrivo" else "Arrivati") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceVariant,
                    labelColor = if (isSelected) colorScheme.primary else colorScheme.onSurface
                )
            )
        }
    }
}

private fun formatRestockDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "n/d"
    return raw.replace('T', ' ').replace("Z", "").trim()
}

private fun buildSideMenuSectionsFromCategories(categories: List<Category>): List<SideMenuSection> {
    if (categories.isEmpty()) return emptyList()
    val parentNameFallback = mapOf(
        "1" to "Casa",
        "2" to "Cura Personale",
        "3" to "Carne",
        "4" to "Pesce",
        "5" to "Verdura",
        "6" to "Frutta",
        "7" to "Bevande"
    )
    val grouped = categories.groupBy { it.parentId }
    return grouped
        .toSortedMap(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
        .map { (parentId, cats) ->
            val title = cats.firstOrNull { !it.parentName.isNullOrBlank() }?.parentName
                ?: parentNameFallback[parentId]
                ?: "Altro"
            SideMenuSection(
                id = parentId ?: "parent-none",
                parentId = parentId,
                title = title,
                entries = cats.sortedBy { it.nome }.map { cat ->
                    SideMenuEntry(
                        id = cat.id,
                        title = cat.nome
                    )
                }
            )
        }
}
