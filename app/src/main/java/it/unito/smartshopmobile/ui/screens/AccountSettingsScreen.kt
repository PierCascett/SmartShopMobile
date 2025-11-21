package it.unito.smartshopmobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.unito.smartshopmobile.data.datastore.AccountPreferences

@Composable
fun AccountSettingsScreen(
    preferences: AccountPreferences,
    onSaveProfile: (String, String, String) -> Unit,
    onSaveBackend: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nome by rememberSaveable(preferences.nome) { mutableStateOf(preferences.nome) }
    var cognome by rememberSaveable(preferences.cognome) { mutableStateOf(preferences.cognome) }
    var indirizzo by rememberSaveable(preferences.indirizzoSpedizione) { mutableStateOf(preferences.indirizzoSpedizione) }
    var host by rememberSaveable(preferences.backendHost) { mutableStateOf(preferences.backendHost) }
    var port by rememberSaveable(preferences.backendPort) { mutableStateOf(preferences.backendPort) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Impostazioni account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        ProfileCard(
            nome = nome,
            cognome = cognome,
            indirizzo = indirizzo,
            onNomeChange = { nome = it },
            onCognomeChange = { cognome = it },
            onIndirizzoChange = { indirizzo = it },
            onSave = { onSaveProfile(nome, cognome, indirizzo) }
        )
        BackendCard(
            host = host,
            port = port,
            onHostChange = { host = it },
            onPortChange = { port = it },
            onSave = { onSaveBackend(host.trim(), port.trim().ifBlank { "3000" }) }
        )
    }
}

@Composable
private fun ProfileCard(
    nome: String,
    cognome: String,
    indirizzo: String,
    onNomeChange: (String) -> Unit,
    onCognomeChange: (String) -> Unit,
    onIndirizzoChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Profilo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = nome,
                onValueChange = onNomeChange,
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cognome,
                onValueChange = onCognomeChange,
                label = { Text("Cognome") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = indirizzo,
                onValueChange = onIndirizzoChange,
                label = { Text("Indirizzo di spedizione") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSave) { Text("Salva dati") }
            }
        }
    }
}

@Composable
private fun BackendCard(
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Rete backend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Il server viene rilevato automaticamente tramite broadcast UDP. Modifica solo se la connessione fallisce.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Host backend (auto-rilevato)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Porta") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSave) { Text("Applica server") }
            }
        }
    }
}
