/**
 * EmployeeScreen.kt (Canvas Native)
 *
 * View Layer (Jetpack Compose)
 * - Schermata Employee con mappa 2D nativa (Canvas)
 * - Nessun WebView/HTML/SVG: solo Compose
 */
package it.unito.smartshopmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.ui.components.StoreMapCanvas
import it.unito.smartshopmobile.viewModel.AisleProduct
import it.unito.smartshopmobile.viewModel.EmployeeViewModel
import it.unito.smartshopmobile.viewModel.StoreAisle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip

@Composable
fun EmployeeScreen(
    modifier: Modifier = Modifier,
    viewModel: EmployeeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mappa Supermercato",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tocca una corsia sulla mappa per vedere i prodotti disponibili.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // Mappa nativa con Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            val background = it.unito.smartshopmobile.ui.map.rememberAssetImage("map/supermarket_resized.png")
            StoreMapCanvas(
                selectedAisleId = uiState.selectedAisleId,
                onAisleClick = { aisleId ->
                    viewModel.selectAisle(aisleId)
                },
                background = background,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(8.dp)
            )
        }

        when {
            uiState.isLoadingAisles -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.aislesError != null -> {
                Text(
                    uiState.aislesError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Pannello dettagli corsia selezionata
        SelectedAislePanel(selected = uiState.selectedAisle)

        OrdersPanel(
            orders = uiState.orders,
            isLoading = uiState.isLoadingOrders,
            error = uiState.ordersError,
            onRefresh = { viewModel.refreshOrders() }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SelectedAislePanel(selected: StoreAisle?) {
    if (selected == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nessuna corsia selezionata", fontWeight = FontWeight.SemiBold)
                Text(
                    "Tocca una corsia sulla mappa per visualizzare i prodotti disponibili.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Corsia ${selected.id} · ${selected.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = selected.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()
            Text("Prodotti disponibili (${selected.products.size})", style = MaterialTheme.typography.labelLarge)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(selected.products) { product ->
                    ProductChip(product)
                }
            }
        }
    }
}

@Composable
private fun ProductChip(product: AisleProduct) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${"%.2f".format(product.price)} €",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (product.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    product.tags.take(2).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
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
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ordini clienti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onRefresh, enabled = !isLoading) {
                    Text("Aggiorna")
                }
            }
            if (isLoading) {
                CircularProgressIndicator()
                return@Column
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
                return@Column
            }
            if (orders.isEmpty()) {
                Text("Nessun ordine al momento")
                return@Column
            }
            orders.forEach { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Ordine #${order.idOrdine} - ${order.stato}", fontWeight = FontWeight.SemiBold)
                        Text("Cliente: ${order.nomeCliente} ${order.cognomeCliente} (${order.emailCliente})")
                        Text("Totale: ${"%.2f".format(order.totale)} €")
                        Text("Articoli: ${order.righe.sumOf { it.quantita }}")
                        if (order.righe.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            order.righe.take(3).forEach { line ->
                                Text(
                                    "${line.nomeProdotto} x${line.quantita} - ${"%.2f".format(line.prezzoTotale)} €",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (order.righe.size > 3) {
                                Text("+${order.righe.size - 3} altri articoli", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
