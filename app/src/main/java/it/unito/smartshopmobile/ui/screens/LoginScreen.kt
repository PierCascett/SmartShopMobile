/**
 * LoginScreen.kt
 *
 * RUOLO MVVM: View Layer (UI - Jetpack Compose)
 * - Schermata di autenticazione dell'applicazione
 * - Punto di ingresso per Customer, Employee e Manager
 * - Puramente presentazionale: stato dal LoginViewModel
 *
 * RESPONSABILITÀ:
 * - Rendering form di login (email, password)
 * - Visualizzare stato loading e messaggi errore
 * - Inviare intent login al LoginViewModel
 * - Navigare alla schermata corretta in base al ruolo utente
 * - NO validazione: delegata al ViewModel
 *
 * PATTERN: MVVM - View
 * - Composable stateless: riceve ViewModel come parametro
 * - Osserva stato tramite .value (MutableState)
 * - Callback per eventi (onLoginClick)
 * - Navigazione condizionale tramite LaunchedEffect
 *
 * COMPONENTI:
 * - LoginScreen: schermata principale
 * - EmailField: input email con validazione visuale
 * - PasswordField: input password con toggle visibilità
 * - LoginButton: pulsante con stato loading
 * - GoogleSignInButton: (mock) autenticazione Google
 */
package it.unito.smartshopmobile.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.unito.smartshopmobile.R
import it.unito.smartshopmobile.data.model.UserRole
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.viewModel.LoginViewModel
import kotlinx.coroutines.launch

// LoginScreenMVVM: UI che usa il ViewModel per stato e azioni
@Composable
fun LoginScreenMVVM(
    viewModel: LoginViewModel,
    onLoginSuccess: (user: User, role: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // stato locale per la visibilità della password e per il ruolo scelto
    val passwordVisible = remember { mutableStateOf(false) }
    var isRegister by rememberSaveable { mutableStateOf(false) }

    // mostro errorMessage del ViewModel
    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // quando loginSuccessUser cambia, notifico l'Activity con il ruolo reale
    LaunchedEffect(viewModel.loginSuccessUser.value) {
        viewModel.loginSuccessUser.value?.let { user ->
            val role = when (user.ruolo.lowercase()) {
                "responsabile" -> UserRole.MANAGER
                "dipendente" -> UserRole.EMPLOYEE
                else -> UserRole.CUSTOMER
            }
            onLoginSuccess(user, role)
            viewModel.clearLoginSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_no_centro),
                        contentDescription = "Logo SmartShop",
                        modifier = Modifier.height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Smart") }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("Shop") }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // email (dal ViewModel)
                    OutlinedTextField(
                        value = viewModel.email.value,
                        onValueChange = { viewModel.setEmail(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isRegister) {
                        OutlinedTextField(
                            value = viewModel.nome.value,
                            onValueChange = { viewModel.nome.value = it },
                            label = { Text("Nome") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Nome") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.cognome.value,
                            onValueChange = { viewModel.cognome.value = it },
                            label = { Text("Cognome") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Cognome") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.telefono.value,
                            onValueChange = { viewModel.telefono.value = it },
                            label = { Text("Telefono (opzionale)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Telefono") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // password (dal ViewModel) con toggle visibilità locale
                    OutlinedTextField(
                        value = viewModel.password.value,
                        onValueChange = { viewModel.setPassword(it) },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                                Icon(
                                    imageVector = if (passwordVisible.value) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible.value) "Nascondi password" else "Mostra password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    // loader o pulsante
                    if (viewModel.isLoading.value) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = {
                                if (isRegister) viewModel.register() else viewModel.login()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isRegister) "Registrati" else "Accedi")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // azioni secondarie
                    TextButton(onClick = { isRegister = !isRegister }) {
                        Text(if (isRegister) "Hai già un account? Accedi" else "Registrati")
                    }
                }
            }
        }
    }
}

// Variante per preview senza ViewModel
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String, role: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_no_centro),
                        contentDescription = "Logo SmartShop",
                        modifier = Modifier.height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Smart") }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("Shop") }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        label = { Text("Email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                                Icon(
                                    imageVector = if (passwordVisible.value) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible.value) "Nascondi password" else "Mostra password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (email.value.isBlank() || password.value.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Inserisci email e password") }
                            } else {
                                onLogin(email.value.trim(), password.value, UserRole.CUSTOMER)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Accedi")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Funzionalità registrazione non implementata") } }) {
                        Text("Registrati")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun LoginPreviewLight() {
    LoginScreen(onLogin = { _, _, _ -> })
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginPreviewDark() {
    LoginScreen(onLogin = { _, _, _ -> })
}

