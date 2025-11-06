package it.unito.smartshopmobile.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.unito.smartshopmobile.viewModel.LoginViewModel
import kotlinx.coroutines.launch

// LoginScreenMVVM: UI che usa il ViewModel per stato e azioni
@Composable
fun LoginScreenMVVM(
    viewModel: LoginViewModel,
    onLoginSuccess: (email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // stato locale per la visibilità della password
    val passwordVisible = remember { mutableStateOf(false) }

    // mostro errorMessage del ViewModel
    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // quando loginSuccessEmail cambia, notifico l'Activity
    LaunchedEffect(viewModel.loginSuccessEmail.value) {
        viewModel.loginSuccessEmail.value?.let { email ->
            onLoginSuccess(email)
            viewModel.clearLoginSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // titolo
            Text(text = "Benvenuto in SmartShop", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // email (dal ViewModel)
            OutlinedTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.setEmail(it) },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // password (dal ViewModel) con toggle visibilità locale
            OutlinedTextField(
                value = viewModel.password.value,
                onValueChange = { viewModel.setPassword(it) },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Text(if (passwordVisible.value) "Nascondi" else "Mostra")
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // loader o pulsante
            if (viewModel.isLoading.value) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Accedi")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // registrati (placeholder)
            TextButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Funzionalità registrazione non implementata") } }) {
                Text("Registrati")
            }
        }
    }
}

// Variante per preview senza ViewModel
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // titolo
            Text(text = "Benvenuto in SmartShop", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // email locale
            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // password locale
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Text(if (passwordVisible.value) "Nascondi" else "Mostra")
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.value.isBlank() || password.value.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Inserisci email e password") }
                    } else {
                        onLogin(email.value.trim(), password.value)
                    }
                },
                modifier = Modifier.fillMaxWidth()
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun LoginPreviewLight() {
    LoginScreen(onLogin = { _, _ -> })
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginPreviewDark() {
    LoginScreen(onLogin = { _, _ -> })
}
