package it.unito.smartshopmobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.unito.smartshopmobile.viewModel.ManagerUiState
import it.unito.smartshopmobile.viewModel.ManagerViewModel

@Composable
fun ManagerScreen(
    modifier: Modifier = Modifier,
    viewModel: ManagerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RestockForm(state = state, onValueChange = viewModel::updateForm, onSubmit = { viewModel.submitRestock() })
        RestockList(state = state, onRefresh = viewModel::refreshRestocks)
    }
}

@Composable
private fun RestockForm(
    state: ManagerUiState,
    onValueChange: (productId: String?, supplierId: String?, quantity: String?, eta: String?) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nuovo riordino", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.prodottoId,
                onValueChange = { onValueChange(it, null, null, null) },
                label = { Text("ID prodotto (es. prd-01)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.fornitoreId,
                onValueChange = { onValueChange(null, it, null, null) },
                label = { Text("ID fornitore") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.quantita,
                onValueChange = { onValueChange(null, null, it, null) },
                label = { Text("Quantita") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.dataArrivoPrevista,
                onValueChange = { onValueChange(null, null, null, it) },
                label = { Text("Data arrivo prevista (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
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
private fun RestockList(
    state: ManagerUiState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RowHeader(onRefresh = onRefresh, isLoading = state.isLoading)
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            state.restocks.forEach { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Riordino #${r.idRiordino} - ${r.prodottoNome}", style = MaterialTheme.typography.labelLarge)
                        Text("Fornitore: ${r.fornitoreNome} | Quantita: ${r.quantitaOrdinata}")
                        Text("Arrivo previsto: ${r.dataArrivoPrevista ?: "n/d"} | Arrivato: ${if (r.arrivato) "Si" else "No"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RowHeader(onRefresh: () -> Unit, isLoading: Boolean) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Riordini magazzino", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onRefresh, enabled = !isLoading) {
            Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna")
        }
    }
}
