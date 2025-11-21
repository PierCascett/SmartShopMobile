// EmployeeScreen.kt - Compose screen for employee UI (cleaned header)
package it.unito.smartshopmobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.ui.components.StoreMapCanvas
import it.unito.smartshopmobile.ui.map.rememberAssetImage
import it.unito.smartshopmobile.viewModel.AisleProduct
import it.unito.smartshopmobile.viewModel.EmployeeViewModel
import it.unito.smartshopmobile.viewModel.OrderFilter
import it.unito.smartshopmobile.viewModel.StoreAisle
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.ImageBitmap
import it.unito.smartshopmobile.viewModel.EmployeeUiState
private enum class EmployeeTab(val label: String, val icon: ImageVector) {
    PICKING("Mappa", Icons.Filled.Map),
    ORDERS("Storico", Icons.Filled.List),
    CLAIM("Assegna", Icons.Filled.Assignment)
}

@Composable
fun EmployeeScreen(modifier: Modifier = Modifier, viewModel: EmployeeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val backgroundImage = rememberAssetImage("map/supermarket_resized.png")
    val aislesError = state.aislesError
    var selectedTab by rememberSaveable { mutableStateOf(EmployeeTab.PICKING) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0.dp)) {
                EmployeeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Padding personalizzato: niente top extra da innerPadding, manteniamo solo bottom per evitare grande spazio vuoto
        val contentModifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = innerPadding.calculateBottomPadding()
        )
        when (selectedTab) {
            EmployeeTab.PICKING -> PickingTab(
                state = state,
                backgroundImage = backgroundImage,
                aislesError = aislesError,
                onSelectAisle = viewModel::selectAisle,
                onTogglePicked = viewModel::togglePicked,
                onMarkCompleted = viewModel::markOrderCompleted,
                onMarkShipped = viewModel::markOrderShipped,
                onMarkCanceled = viewModel::markOrderCanceled,
                onProductClick = viewModel::showProductDetail,
                onOpenSelection = { selectedTab = EmployeeTab.CLAIM },
                onRefreshOrders = viewModel::refreshOrders,
                onReleaseOrder = {
                    viewModel.dropActiveOrder()
                    selectedTab = EmployeeTab.CLAIM
                },
                modifier = contentModifier
            )
            EmployeeTab.ORDERS -> OrdersTab(
                state = state,
                onFilterChange = viewModel::setOrderFilter,
                onRefresh = viewModel::refreshOrders,
                onOrderClick = viewModel::toggleOrder,
                onMarkShipped = viewModel::markOrderShipped,
                onMarkCompleted = viewModel::markOrderCompleted,
                onMarkCanceled = viewModel::markOrderCanceled,
                modifier = contentModifier
            )
            EmployeeTab.CLAIM -> ClaimTab(
                state = state,
                onTakeOrder = viewModel::startOrder,
                onSelectTab = { selectedTab = EmployeeTab.PICKING },
                onRefreshOrders = viewModel::refreshOrders,
                modifier = contentModifier
            )
        }
    }

    state.selectedProduct?.let { product -> ProductDetailDialog(product = product, onDismiss = viewModel::dismissProductDetail) }
}

@Composable
private fun PickingTab(
    state: EmployeeUiState,
    backgroundImage: ImageBitmap?,
    aislesError: String?,
    onSelectAisle: (String) -> Unit,
    onTogglePicked: (Int) -> Unit,
    onMarkCompleted: (Int) -> Unit,
    onMarkShipped: (Int) -> Unit,
    onMarkCanceled: (Int) -> Unit,
    onProductClick: (AisleProduct) -> Unit,
    onOpenSelection: () -> Unit,
    onRefreshOrders: () -> Unit,
    onReleaseOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activeOrder = state.activeOrder
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mappa e preparazione", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Tocca una corsia per vedere cosa c'e' su quello scaffale e segnare i prodotti man mano che li recuperi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(8.dp)) {
                StoreMapCanvas(
                    selectedAisleId = state.selectedAisleId,
                    onAisleClick = onSelectAisle,
                    background = backgroundImage,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.isLoadingAisles) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        when {
            state.isLoadingAisles -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            aislesError != null -> Text(aislesError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        SelectedAislePanel(selected = state.selectedAisle, onProductClick = onProductClick)

        if (activeOrder == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nessun ordine in preparazione", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Vai nella tab \"Assegna\" per prendere in carico un ordine e vedere sulla mappa dove sono i prodotti.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onOpenSelection) { Text("Scegli un ordine") }
                }
            }
        } else {
            OrderPickingPanel(
                order = activeOrder,
                pickedLines = state.pickedLines,
                productShelfMap = state.productShelfMap,
                updatingOrderId = state.updatingOrderId,
                onTogglePicked = onTogglePicked,
                onJumpToAisle = onSelectAisle,
                onMarkCompleted = onMarkCompleted,
                onMarkShipped = onMarkShipped,
                onMarkCanceled = onMarkCanceled,
                onSetInPreparation = null,
                onReleaseActive = onReleaseOrder
            )
        }
    }
}

@Composable
private fun OrdersTab(
    state: EmployeeUiState,
    onFilterChange: (OrderFilter) -> Unit,
    onRefresh: () -> Unit,
    onOrderClick: (Int) -> Unit,
    onMarkShipped: (Int) -> Unit,
    onMarkCompleted: (Int) -> Unit,
    onMarkCanceled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageSize = 4
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var dialogOrder by rememberSaveable { mutableStateOf<Order?>(null) }
    val filtered = when (state.orderFilter) {
        OrderFilter.ACTIVE -> state.orders.filter { !orderIsFinal(it.stato) }
        OrderFilter.COMPLETED -> state.orders.filter { orderIsFinal(it.stato) }
    }
    val activeCount = state.orders.count { !orderIsFinal(it.stato) }
    val completedCount = state.orders.size - activeCount
    val pageCount = ((filtered.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    currentPage = currentPage.coerceIn(0, pageCount - 1)
    val pageOrders: List<Order> = filtered.drop(currentPage * pageSize).take(pageSize)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrderFilterToggle(
                selectedFilter = state.orderFilter,
                onFilterChange = onFilterChange,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.isLoadingOrders,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .defaultMinSize(minHeight = 30.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna ordini", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiorna")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Da gestire: $activeCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Conclusi: $completedCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when {
            state.isLoadingOrders -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.ordersError != null -> Text(state.ordersError, color = MaterialTheme.colorScheme.error)
            pageOrders.isEmpty() -> Text("Nessun ordine", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    items(pageOrders, key = { order: Order -> order.idOrdine }) { order ->
                        SimpleOrderCard(
                            order = order,
                            isExpanded = false,
                            updatingOrderId = state.updatingOrderId,
                            onOrderClick = { dialogOrder = order; onOrderClick(order.idOrdine) },
                            onMarkShipped = { onMarkShipped(order.idOrdine) },
                            onMarkCompleted = { onMarkCompleted(order.idOrdine) },
                            onMarkCanceled = { onMarkCanceled(order.idOrdine) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pagina ${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.labelLarge
            )
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

        dialogOrder?.let { order ->
            AlertDialog(
                onDismissRequest = { dialogOrder = null },
                confirmButton = {
                    TextButton(onClick = { dialogOrder = null }) { Text("Chiudi") }
                },
                title = { Text("Ordine #${order.idOrdine}", fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cliente: ${order.nomeCliente} ${order.cognomeCliente}")
                        Text("Stato: ${orderStatusLabel(order.stato)}")
                        Text("Totale: ${formatEuro(order.totale)} | Articoli: ${order.righe.sumOf { it.quantita }}")
                        Text("Consegna: ${order.metodoConsegna}")
                        if (order.righe.isEmpty()) {
                            Text("Nessuna riga disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            order.righe.forEach { line ->
                                Text("${line.nomeProdotto} x${line.quantita} - ${formatEuro(line.prezzoTotale)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ClaimTab(
    state: EmployeeUiState,
    onTakeOrder: (Int) -> Unit,
    onSelectTab: () -> Unit,
    onRefreshOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Assegna ordine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            OutlinedButton(
                onClick = onRefreshOrders,
                enabled = !state.isLoadingOrders,
                modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna ordini", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiorna")
            }
        }
        when {
            state.isLoadingOrders -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.ordersError != null -> Text(state.ordersError, color = MaterialTheme.colorScheme.error)
            else -> {
                val claimable = state.orders.filter { !orderIsFinal(it.stato) }
                if (claimable.isEmpty()) {
                    Text("Nessun ordine da prendere in carico", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(claimable, key = { it.idOrdine }) { order ->
                            ClaimOrderCard(
                                order = order,
                                isActive = state.activeOrderId == order.idOrdine,
                                onTake = {
                                    onTakeOrder(order.idOrdine)
                                    onSelectTab()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersPanel(
    orders: List<Order>,
    isLoading: Boolean,
    error: String?,
    expandedOrderId: Int?,
    updatingOrderId: Int?,
    actionError: String?,
    selectedFilter: OrderFilter,
    onFilterChange: (OrderFilter) -> Unit,
    onRefresh: () -> Unit,
    onOrderClick: (Int) -> Unit,
    onMarkShipped: (Int) -> Unit,
    onMarkCompleted: (Int) -> Unit,
    onMarkCanceled: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ordini clienti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Aggiorna") }
            }

            when {
                isLoading -> { CircularProgressIndicator(); return@Column }
                error != null -> { Text(error, color = MaterialTheme.colorScheme.error); return@Column }
                orders.isEmpty() -> { Text("Nessun ordine al momento"); return@Column }
            }

            OrderFilterToggle(selectedFilter = selectedFilter, onFilterChange = onFilterChange)

            val activeOrders = orders.filter { !orderIsFinal(it.stato) }
            val completedOrders = orders.filter { orderIsFinal(it.stato) }
            val sectionTitle: String
            val visibleOrders: List<Order>
            val showActions: Boolean
            val emptyMessage: String

            when (selectedFilter) {
                OrderFilter.ACTIVE -> {
                    sectionTitle = "In gestione (${activeOrders.size})"
                    visibleOrders = activeOrders
                    showActions = true
                    emptyMessage = "Nessun ordine in gestione"
                }
                OrderFilter.COMPLETED -> {
                    sectionTitle = "Ordini conclusi (${completedOrders.size})"
                    visibleOrders = completedOrders
                    showActions = false
                    emptyMessage = "Nessun ordine concluso"
                }
            }

            if (visibleOrders.isEmpty()) Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else OrderSection(title = sectionTitle, orders = visibleOrders, expandedOrderId = expandedOrderId, updatingOrderId = updatingOrderId, showActions = showActions, actionError = if (showActions) actionError else null, onOrderClick = onOrderClick, onMarkShipped = onMarkShipped, onMarkCompleted = onMarkCompleted, onMarkCanceled = onMarkCanceled)
        }
    }
}

@Composable
private fun SelectedAislePanel(selected: StoreAisle?, onProductClick: (AisleProduct) -> Unit) {
    if (selected == null) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nessuna corsia selezionata", fontWeight = FontWeight.SemiBold)
                Text("Tocca una corsia sulla mappa per visualizzare i prodotti disponibili.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Corsia ${selected.id} - ${selected.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = selected.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Prodotti disponibili (${selected.products.size})", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) { items(selected.products) { product -> ProductChip(product, onProductClick = onProductClick) } }
        }
    }
}

@Composable
private fun ProductChip(product: AisleProduct, onProductClick: (AisleProduct) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.width(190.dp).clickable { onProductClick(product) }) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatEuro(product.price), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (product.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    product.tags.take(3).forEach { tag ->
                        Text(text = tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailDialog(product: AisleProduct, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }, title = { Text(product.name, fontWeight = FontWeight.SemiBold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Marca: ${product.brand}")
            Text("Prezzo: ${formatEuro(product.price)}")
            if (!product.description.isNullOrBlank()) Text(product.description) else Text("Nessuna descrizione disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (product.tags.isNotEmpty()) Text("Tag: ${product.tags.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    })
}

@Composable
private fun OrderPickingPanel(
    order: Order,
    pickedLines: Set<Int>,
    productShelfMap: Map<String, String>,
    updatingOrderId: Int?,
    onTogglePicked: (Int) -> Unit,
    onJumpToAisle: (String) -> Unit,
    onMarkCompleted: (Int) -> Unit,
    onMarkShipped: (Int) -> Unit,
    onMarkCanceled: (Int) -> Unit,
    onSetInPreparation: ((Int) -> Unit)?,
    onReleaseActive: (() -> Unit)?
) {
    val statusLabel = orderStatusLabel(order.stato)
    val allPicked = order.righe.isNotEmpty() && order.righe.all { pickedLines.contains(it.idRiga) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Ordine #${order.idOrdine}", fontWeight = FontWeight.SemiBold)
                    Text("Cliente: ${order.nomeCliente} ${order.cognomeCliente}", style = MaterialTheme.typography.bodySmall)
                }
                Text(statusLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Text("Totale: ${formatEuro(order.totale)} | Articoli: ${order.righe.sumOf { it.quantita }}")
            Text("Metodo: ${order.metodoConsegna}", style = MaterialTheme.typography.bodySmall)

            if (order.righe.isEmpty()) {
                Text("Nessuna riga ordine disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OrderChecklist(
                    order = order,
                    pickedLines = pickedLines,
                    productShelfMap = productShelfMap,
                    onToggle = onTogglePicked,
                    onJumpToAisle = onJumpToAisle
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSetInPreparation != null && !order.stato.equals("IN_PREPARAZIONE", true)) {
                    TextButton(
                        onClick = { onSetInPreparation(order.idOrdine) },
                        enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato)
                    ) { Text("Metti in preparazione") }
                }
                if (order.metodoConsegna.equals("DOMICILIO", true)) {
                    OutlinedButton(
                        onClick = { onMarkShipped(order.idOrdine) },
                        enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato) && !order.stato.equals("SPEDITO", true)
                    ) { Text("Segna spedito") }
                }
                  if (onReleaseActive != null) {
                      OutlinedButton(
                          onClick = onReleaseActive,
                          enabled = updatingOrderId != order.idOrdine
                      ) { Text("Annulla assegnazione") }
                  } else {
                      OutlinedButton(
                          onClick = { onMarkCanceled(order.idOrdine) },
                          enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato)
                      ) { Text("Annulla") }
                  }
                Button(
                    onClick = { onMarkCompleted(order.idOrdine) },
                    enabled = updatingOrderId != order.idOrdine && !order.stato.equals("CONCLUSO", true) && !orderIsFinal(order.stato) && allPicked
                ) { Text(if (allPicked) "Segna concluso" else "Completa checklist") }
            }
            if (updatingOrderId == order.idOrdine) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun OrderChecklist(
    order: Order,
    pickedLines: Set<Int>,
    productShelfMap: Map<String, String>,
    onToggle: (Int) -> Unit,
    onJumpToAisle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Checklist articoli", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        order.righe.forEach { line ->
            val picked = pickedLines.contains(line.idRiga)
            val aisle = productShelfMap[line.idProdotto]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = picked, onCheckedChange = { onToggle(line.idRiga) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${line.nomeProdotto} x${line.quantita}", fontWeight = FontWeight.Medium)
                        Text(formatEuro(line.prezzoTotale), style = MaterialTheme.typography.bodySmall)
                        aisle?.let {
                            Text("Corsia $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    aisle?.let {
                        TextButton(onClick = { onJumpToAisle(it) }) { Text("Vedi") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimOrderCard(order: Order, isActive: Boolean, onTake: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ordine #${order.idOrdine}", fontWeight = FontWeight.SemiBold)
                Text("Cliente: ${order.nomeCliente} ${order.cognomeCliente}", style = MaterialTheme.typography.bodySmall)
                Text("Totale: ${formatEuro(order.totale)} • Consegna: ${order.metodoConsegna}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onTake,
                modifier = Modifier.defaultMinSize(minWidth = 88.dp, minHeight = 34.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = if (isActive) "Continua" else "Prendi in carico"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun OrderSection(title: String, orders: List<Order>, expandedOrderId: Int?, updatingOrderId: Int?, showActions: Boolean, actionError: String?, onOrderClick: (Int) -> Unit, onMarkShipped: (Int) -> Unit, onMarkCompleted: (Int) -> Unit, onMarkCanceled: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (orders.isEmpty()) {
            Text("Nessun ordine in questa sezione", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val chunks = orders.chunked(4)
            val listState = rememberLazyListState()
            val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            LazyRow(state = listState, flingBehavior = flingBehavior, horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(chunks) { chunk ->
                    OrderClusterCard(orders = chunk, expandedOrderId = expandedOrderId, updatingOrderId = updatingOrderId, showActions = showActions, actionError = actionError, onOrderClick = onOrderClick, onMarkShipped = onMarkShipped, onMarkCompleted = onMarkCompleted, onMarkCanceled = onMarkCanceled)
                }
            }
        }
    }
}

@Composable
private fun OrderClusterCard(orders: List<Order>, expandedOrderId: Int?, updatingOrderId: Int?, showActions: Boolean, actionError: String?, onOrderClick: (Int) -> Unit, onMarkShipped: (Int) -> Unit, onMarkCompleted: (Int) -> Unit, onMarkCanceled: (Int) -> Unit) {
    Card(modifier = Modifier.widthIn(min = 280.dp, max = 340.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            orders.forEach { order ->
                OrderEntry(order = order, isExpanded = order.idOrdine == expandedOrderId, updatingOrderId = updatingOrderId, showActions = showActions, actionError = actionError, onOrderClick = { onOrderClick(order.idOrdine) }, onMarkShipped = { onMarkShipped(order.idOrdine) }, onMarkCompleted = { onMarkCompleted(order.idOrdine) }, onMarkCanceled = { onMarkCanceled(order.idOrdine) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderEntry(order: Order, isExpanded: Boolean, updatingOrderId: Int?, showActions: Boolean, actionError: String?, onOrderClick: () -> Unit, onMarkShipped: () -> Unit, onMarkCompleted: () -> Unit, onMarkCanceled: () -> Unit) {
    val statusLabel = orderStatusLabel(order.stato)
    val deliveryLabel = if (order.metodoConsegna.equals("DOMICILIO", ignoreCase = true)) "Spesa a domicilio" else "Ritiro nel locker"

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable { onOrderClick() }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Ordine #${order.idOrdine}", fontWeight = FontWeight.SemiBold)
                Text("Cliente: ${order.nomeCliente} ${order.cognomeCliente}", style = MaterialTheme.typography.bodySmall)
            }
            Text(statusLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }

        Text("Totale: ${formatEuro(order.totale)} | Articoli: ${order.righe.sumOf { it.quantita }}")
        Text("Consegna: $deliveryLabel", style = MaterialTheme.typography.bodySmall)

        if (isExpanded) {
            HorizontalDivider()
            order.righe.forEach { line -> Text("${line.nomeProdotto} x${line.quantita} - ${formatEuro(line.prezzoTotale)}", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(4.dp))
            if (order.metodoConsegna.equals("LOCKER", ignoreCase = true)) {
                Text("Locker: ${order.idLocker ?: "da assegnare"}", style = MaterialTheme.typography.bodySmall)
                order.codiceRitiro?.let { Text("Codice ritiro: $it", fontWeight = FontWeight.SemiBold) }
            } else {
                Text("Prepara per rider / consegna a domicilio", style = MaterialTheme.typography.bodySmall)
            }

            if (showActions) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.metodoConsegna.equals("DOMICILIO", ignoreCase = true)) {
                        OutlinedButton(onClick = onMarkShipped, enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato) && order.stato.uppercase() != "SPEDITO", modifier = Modifier.widthIn(min = 140.dp)) { Text("Segna spedito") }
                    }
                    OutlinedButton(onClick = onMarkCanceled, enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato), modifier = Modifier.widthIn(min = 140.dp)) { Text("Annulla") }
                    Button(onClick = onMarkCompleted, enabled = updatingOrderId != order.idOrdine && !order.stato.equals("CONCLUSO", true), modifier = Modifier.widthIn(min = 140.dp)) { Text("Segna concluso") }
                }

                if (updatingOrderId == order.idOrdine) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

        } else {
            Text("Tocca per vedere i dettagli", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SimpleOrderCard(
    order: Order,
    isExpanded: Boolean,
    updatingOrderId: Int?,
    onOrderClick: () -> Unit,
    onMarkShipped: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkCanceled: () -> Unit
) {
    val statusLabel = orderStatusLabel(order.stato)
    val deliveryLabel = if (order.metodoConsegna.equals("DOMICILIO", true)) "Domicilio" else "Locker"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOrderClick() },
        tonalElevation = 1.dp
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
                Text("Ordine #${order.idOrdine}", fontWeight = FontWeight.SemiBold)
                Text(statusLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Text("${order.nomeCliente} ${order.cognomeCliente}", style = MaterialTheme.typography.bodySmall)
            Text("Totale: ${formatEuro(order.totale)} • Articoli: ${order.righe.sumOf { it.quantita }} • $deliveryLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isExpanded) {
                order.righe.forEach { line ->
                    Text(
                        "${line.nomeProdotto} x${line.quantita}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (order.metodoConsegna.equals("DOMICILIO", true)) {
                        TextButton(
                            onClick = onMarkShipped,
                            enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato) && !order.stato.equals("SPEDITO", true)
                        ) { Text("Spedito") }
                    }
                    TextButton(
                        onClick = onMarkCanceled,
                        enabled = updatingOrderId != order.idOrdine && !orderIsFinal(order.stato)
                    ) { Text("Annulla") }
                    TextButton(
                        onClick = onMarkCompleted,
                        enabled = updatingOrderId != order.idOrdine && !order.stato.equals("CONCLUSO", true) && !orderIsFinal(order.stato)
                    ) { Text("Concluso") }
                }
            } else {
                Text("Tocca per dettagli", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun orderIsFinal(status: String): Boolean = when (status.uppercase()) {
    "CONCLUSO", "CONSEGNATO", "ANNULLATO" -> true
    else -> false
}

private fun orderStatusLabel(status: String): String = when (status.uppercase()) {
    "IN_PREPARAZIONE" -> "In preparazione"
    "CONCLUSO" -> "Concluso"
    "SPEDITO" -> "Spedito"
    "CONSEGNATO" -> "Consegnato"
    "ANNULLATO" -> "Annullato"
    else -> status
}

private fun formatEuro(value: Double): String = "\u20AC ${String.format(Locale.ROOT, "%.2f", value)}"

@Composable
private fun OrderFilterToggle(
    selectedFilter: OrderFilter,
    onFilterChange: (OrderFilter) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OrderFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            AssistChip(onClick = { onFilterChange(filter) }, label = { Text(text = if (filter == OrderFilter.ACTIVE) "Da gestire" else "Conclusi") }, colors = AssistChipDefaults.assistChipColors(containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
        }
    }
}
