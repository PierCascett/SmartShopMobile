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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun EmployeeScreen(modifier: Modifier = Modifier, viewModel: EmployeeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val backgroundImage = rememberAssetImage("map/supermarket_resized.png")
    val aislesError = state.aislesError

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Mappa Supermercato", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(text = "Tocca una corsia sulla mappa per vedere i prodotti disponibili.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(8.dp)) {
                StoreMapCanvas(selectedAisleId = state.selectedAisleId, onAisleClick = viewModel::selectAisle, background = backgroundImage, modifier = Modifier.fillMaxSize())
                if (state.isLoadingAisles) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        when {
            state.isLoadingAisles -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            aislesError != null -> Text(aislesError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        SelectedAislePanel(selected = state.selectedAisle, onProductClick = viewModel::showProductDetail)

        OrdersPanel(
            orders = state.orders,
            isLoading = state.isLoadingOrders,
            error = state.ordersError,
            expandedOrderId = state.expandedOrderId,
            updatingOrderId = state.updatingOrderId,
            actionError = state.orderActionError,
            selectedFilter = state.orderFilter,
            onFilterChange = viewModel::setOrderFilter,
            onRefresh = viewModel::refreshOrders,
            onOrderClick = viewModel::toggleOrder,
            onMarkShipped = viewModel::markOrderShipped,
            onMarkCompleted = viewModel::markOrderCompleted,
            onMarkCanceled = viewModel::markOrderCanceled
        )
    }

    state.selectedProduct?.let { product -> ProductDetailDialog(product = product, onDismiss = viewModel::dismissProductDetail) }
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
            Text(text = "Corsia ${selected.id} · ${selected.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        Text("Totale: ${formatEuro(order.totale)} • Articoli: ${order.righe.sumOf { it.quantita }}")
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

private fun orderIsFinal(status: String): Boolean = when (status.uppercase()) {
    "CONCLUSO", "CONSEGNATO", "ANNULLATO" -> true
    else -> false
}

private fun orderStatusLabel(status: String): String = when (status.uppercase()) {
    "IN_PREPARAZIONE" -> "In preparazione"
    "CONCLUSO" -> "Concluso"
    "SPEDITO" -> "Spedito"
    "CREATO" -> "Creato"
    "CONSEGNATO" -> "Consegnato"
    "ANNULLATO" -> "Annullato"
    else -> status
}

private fun formatEuro(value: Double): String = "€ ${String.format(Locale.ROOT, "%.2f", value)}"

@Composable
private fun OrderFilterToggle(selectedFilter: OrderFilter, onFilterChange: (OrderFilter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OrderFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            AssistChip(onClick = { onFilterChange(filter) }, label = { Text(text = if (filter == OrderFilter.ACTIVE) "In gestione" else "Conclusi") }, colors = AssistChipDefaults.assistChipColors(containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
        }
    }
}
