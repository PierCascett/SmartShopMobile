/**
 * AccountSettingsScreen.kt
 *
 * MVVM: View Layer - UI impostazioni profilo
 *
 * FUNZIONAMENTO:
 * - Form modifica profilo (nome, cognome, email, telefono)
 * - Upload foto profilo con image picker
 * - Validazione input (email pattern)
 * - Salvataggio su API + aggiorna SessionDataStore
 *
 * PATTERN MVVM:
 * - View: rendering form profilo
 * - Validazione locale UI-side
 * - Callback: onSaveProfile, onUploadPhoto
 * - ActivityResultLauncher: selezione immagine
 */
package it.unito.smartshopmobile.ui.screens

import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import it.unito.smartshopmobile.data.datastore.AccountPreferences
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import java.util.UUID

@Composable
fun AccountSettingsScreen(
    preferences: AccountPreferences,
    email: String,
    avatarUrl: String?,
    isSaving: Boolean,
    isUploadingPhoto: Boolean,
    onSaveProfile: (String, String, String, String, String) -> Unit,
    onPickNewPhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var nome by rememberSaveable(preferences.nome) { mutableStateOf(preferences.nome) }
    var cognome by rememberSaveable(preferences.cognome) { mutableStateOf(preferences.cognome) }
    var indirizzo by rememberSaveable(preferences.indirizzoSpedizione) { mutableStateOf(preferences.indirizzoSpedizione) }
    var telefono by rememberSaveable(preferences.telefono) { mutableStateOf(preferences.telefono) }
    var currentEmail by rememberSaveable(email) { mutableStateOf(email) }
    LaunchedEffect(
        preferences.nome,
        preferences.cognome,
        preferences.indirizzoSpedizione,
        preferences.telefono,
        email
    ) {
        nome = preferences.nome
        cognome = preferences.cognome
        indirizzo = preferences.indirizzoSpedizione
        telefono = preferences.telefono
        currentEmail = email
    }
    val isEmailValid = remember(currentEmail) {
        Patterns.EMAIL_ADDRESS.matcher(currentEmail.trim()).matches()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileCard(
            nome = nome,
            cognome = cognome,
            indirizzo = indirizzo,
            telefono = telefono,
            email = currentEmail,
            avatarUrl = avatarUrl,
            uploadingPhoto = isUploadingPhoto,
            isEmailValid = isEmailValid,
            onNomeChange = { nome = it },
            onCognomeChange = { cognome = it },
            onIndirizzoChange = { indirizzo = it },
            onTelefonoChange = { telefono = it },
            onEmailChange = { currentEmail = it },
            isSaving = isSaving,
            onPickNewPhoto = onPickNewPhoto,
            onSave = {
                if (isEmailValid) {
                    onSaveProfile(nome, cognome, currentEmail, indirizzo, telefono)
                }
            }
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
    avatarUrl: String?,
    uploadingPhoto: Boolean,
    isEmailValid: Boolean,
    onNomeChange: (String) -> Unit,
    onCognomeChange: (String) -> Unit,
    onIndirizzoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    isSaving: Boolean,
    onPickNewPhoto: (Uri) -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val context = LocalContext.current
        val pickPhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> uri?.let(onPickNewPhoto) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val avatarModel = avatarUrl?.takeIf { it.isNotBlank() }?.let { RetrofitInstance.buildAssetUrl(it) }
            val cacheBuster = remember(avatarUrl) { UUID.randomUUID().toString() }
            val bustedAvatar = remember(avatarModel, cacheBuster) {
                avatarModel?.let { url ->
                    if (url.contains("?")) "$url&v=$cacheBuster" else "$url?v=$cacheBuster"
                }
            }
            val avatarRequest = bustedAvatar?.let {
                ImageRequest.Builder(context)
                    .data(it)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .crossfade(true)
                    .build()
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    key(avatarUrl) {
                        if (avatarRequest != null) {
                            AsyncImage(
                                model = avatarRequest,
                                contentDescription = "Foto profilo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { pickPhotoLauncher.launch("image/*") },
                    enabled = !uploadingPhoto
                ) { Text(if (uploadingPhoto) "Caricamento..." else "Cambia foto") }
                if (uploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isEmailValid,
                singleLine = true
            )
            if (!isEmailValid) {
                Text(
                    "Inserisci un'email valida",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSave, enabled = !isSaving && isEmailValid) { Text(if (isSaving) "Salvataggio..." else "Salva dati") }
            }
        }
    }
}
