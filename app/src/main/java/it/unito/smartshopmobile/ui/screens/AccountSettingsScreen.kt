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
    email: String,
    isSaving: Boolean,
    error: String?,
    success: String?,
    onSaveProfile: (String, String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nome by rememberSaveable(preferences.nome) { mutableStateOf(preferences.nome) }
    var cognome by rememberSaveable(preferences.cognome) { mutableStateOf(preferences.cognome) }
    var indirizzo by rememberSaveable(preferences.indirizzoSpedizione) { mutableStateOf(preferences.indirizzoSpedizione) }
    var telefono by rememberSaveable(preferences.telefono) { mutableStateOf(preferences.telefono) }
    var currentEmail by rememberSaveable(email) { mutableStateOf(email) }

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
            telefono = telefono,
            email = currentEmail,
            onNomeChange = { nome = it },
            onCognomeChange = { cognome = it },
            onIndirizzoChange = { indirizzo = it },
            onTelefonoChange = { telefono = it },
            onEmailChange = { currentEmail = it },
            isSaving = isSaving,
            error = error,
            success = success,
            onSave = { onSaveProfile(nome, cognome, currentEmail, indirizzo, telefono) }
        )
    }
}

@Composable
private fun ProfileCard(
    nome: String,
    cognome: String,
    indirizzo: String,
    telefono: String,
    email: String,
    onNomeChange: (String) -> Unit,
    onCognomeChange: (String) -> Unit,
    onIndirizzoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    isSaving: Boolean,
    error: String?,
    success: String?,
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
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
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
                value = telefono,
                onValueChange = onTelefonoChange,
                label = { Text("Telefono") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = indirizzo,
                onValueChange = onIndirizzoChange,
                label = { Text("Indirizzo di spedizione") },
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            success?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSave, enabled = !isSaving) { Text(if (isSaving) "Salvataggio..." else "Salva dati") }
            }
        }
    }
}
