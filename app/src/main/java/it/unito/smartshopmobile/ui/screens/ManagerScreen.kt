package it.unito.smartshopmobile.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.unito.smartshopmobile.viewModel.ManagerUiState
import it.unito.smartshopmobile.viewModel.ManagerViewModel

private enum class ManagerTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RESTOCK("Effettua riordine", Icons.Filled.Assignment),
    TRANSFER("Trasferisci", Icons.Filled.Refresh),
    LIST("Storico", Icons.Filled.List)
}

@Composable
fun ManagerScreen(
    modifier: Modifier = Modifier,
    viewModel: ManagerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(ManagerTab.RESTOCK) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0.dp)) {
                ManagerTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            ManagerTab.RESTOCK -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
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
                        onSubmit = { viewModel.submitRestock() }
                    )
                }
            }
            ManagerTab.TRANSFER -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TransferForm(
                        state = state,
                        onCategorySelected = viewModel::onCategorySelected,
                        onProductSelected = viewModel::onProductSelected,
                        onShelfSelected = viewModel::onShelfSelected,
                        onQuantityChange = viewModel::onTransferQuantityChanged,
                        onSubmit = viewModel::moveStockToShelf
                    )
                }
            }
            ManagerTab.LIST -> RestockList(
                state = state,
                onRefresh = viewModel::refreshRestocks,
                onShowProduct = { viewModel.showProductDetail(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
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
                    Text("Prezzo:  ${String.format(java.util.Locale.ROOT, "%.2f", product.price)}")
                    if (!product.description.isNullOrBlank()) {
                        Text(product.description!!)
                    }
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
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nuovo riordino", style = MaterialTheme.typography.titleMedium)
            Text("Seleziona categoria", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    val selected = category.id == state.selectedCategoryId
                    AssistChip(
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.nome ?: category.id) },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Text("Prodotti", style = MaterialTheme.typography.labelLarge)
            if (state.availableProducts.isEmpty()) {
                Text("Nessun prodotto in questa categoria", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.availableProducts, key = { it.catalogId }) { product ->
                        val selected = product.id == state.selectedProductId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("€ ${String.format(java.util.Locale.ROOT, "%.2f", product.price)}", color = MaterialTheme.colorScheme.primary)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { onShowProduct(product.id) }) {
                                        Text("Dettagli")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text("Fornitori", style = MaterialTheme.typography.labelLarge)
            if (state.suppliers.isEmpty()) {
                Text("Nessun fornitore disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.suppliers) { supplier ->
                        val selected = supplier.id == state.selectedSupplierId
                        AssistChip(
                            onClick = { onSupplierSelected(supplier.id) },
                            label = { Text(supplier.name) },
                            shape = RoundedCornerShape(50),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.quantity,
                onValueChange = onQuantityChange,
                label = { Text("Quantita") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Arrivo previsto automatico in circa 30 secondi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onSubmit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Invia riordino")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun TransferForm(
    state: ManagerUiState,
    onCategorySelected: (String?) -> Unit,
    onProductSelected: (String) -> Unit,
    onShelfSelected: (Int) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trasferisci dal magazzino agli scaffali", style = MaterialTheme.typography.titleMedium)
            Text("Seleziona categoria", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    val selected = category.id == state.selectedCategoryId
                    AssistChip(
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.nome ?: category.id) },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Text("Prodotti (magazzino -> catalogo)", style = MaterialTheme.typography.labelLarge)
            if (state.availableProducts.isEmpty()) {
                Text("Nessun prodotto in questa categoria", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.availableProducts, key = { it.catalogId }) { product ->
                        val selected = product.id == state.selectedProductId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "Magazzino: ${product.warehouseQuantity} | Catalogo: ${product.catalogQuantity} | Totale: ${product.totalQuantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Text("Scaffale di destinazione", style = MaterialTheme.typography.labelLarge)
            if (state.shelves.isEmpty()) {
                Text("Nessuno scaffale disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.shelves) { shelf ->
                        val selected = shelf.id == state.selectedShelfId
                        AssistChip(
                            onClick = { onShelfSelected(shelf.id) },
                            label = { Text(shelf.nome) },
                            shape = RoundedCornerShape(50),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.transferQuantity,
                onValueChange = onQuantityChange,
                label = { Text("Quantita da spostare") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "I pezzi verranno scalati dal magazzino e aggiunti allo scaffale selezionato.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onSubmit,
                enabled = !state.isTransferring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isTransferring) "Trasferimento..." else "Trasferisci scorte")
            }
            state.transferError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.transferSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RestockList(
    state: ManagerUiState,
    onRefresh: () -> Unit,
    onShowProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riordini magazzino", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna")
                }
            }
            if (state.restocks.isEmpty()) {
                Text("Nessun riordino registrato", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.restocks, key = { it.idRiordino }) { restock ->
                        RestockEntry(restock, onShowProduct)
                    }
                }
            }
        }
    }
}

@Composable
private fun RestockEntry(restock: it.unito.smartshopmobile.data.entity.Restock, onShowProduct: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Riordino #${restock.idRiordino}", fontWeight = FontWeight.SemiBold)
        Text("Prodotto: ${restock.prodottoNome}")
        Text("Fornitore: ${restock.fornitoreNome}")
        Text("Quantita: ${restock.quantitaOrdinata}")
        Text("Arrivo previsto: ${restock.dataArrivoPrevista ?: "n/d"}")
        Text("Stato: ${if (restock.arrivato) "Arrivato" else "In arrivo"}", color = if (restock.arrivato) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onShowProduct(restock.idProdotto) }) {
            Text("Dettagli prodotto")
        }
    }
}

