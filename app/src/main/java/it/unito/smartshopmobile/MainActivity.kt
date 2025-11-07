// Activity principale: avvio tema e schermata di login
// - uso `LoginViewModel` con `by viewModels()`
// - mostro `LoginScreenMVVM` e gestisco il successo con un Toast

package it.unito.smartshopmobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import it.unito.smartshopmobile.data.model.UserRole
import it.unito.smartshopmobile.ui.screens.CatalogScreen
import it.unito.smartshopmobile.ui.screens.LoginScreenMVVM
import it.unito.smartshopmobile.ui.screens.LoginScreen
import it.unito.smartshopmobile.ui.screens.EmployeeScreen
import it.unito.smartshopmobile.ui.screens.ManagerScreen
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.CatalogUiState
import it.unito.smartshopmobile.viewModel.CatalogViewModel
import it.unito.smartshopmobile.viewModel.LoginViewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val catalogViewModel: CatalogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartShopMobileTheme {
                var loggedUser by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
                val catalogState by catalogViewModel.uiState.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    if (loggedUser == null) {
                        LoginScreenMVVM(
                            viewModel = loginViewModel,
                            modifier = contentModifier,
                            onLoginSuccess = { email, role ->
                                loggedUser = email
                                selectedRole = role
                                Toast.makeText(this, "Accesso: $email", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        ContentWithSessionBar(
                            modifier = contentModifier,
                            email = loggedUser ?: "",
                            role = selectedRole,
                            onLogout = {
                                loggedUser = null
                                selectedRole = null
                            },
                            catalogState = catalogState,
                            catalogViewModel = catalogViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentWithSessionBar(
    modifier: Modifier,
    email: String,
    role: UserRole?,
    onLogout: () -> Unit,
    catalogState: CatalogUiState,
    catalogViewModel: CatalogViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SessionBar(email = email, onLogout = onLogout)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            when (role) {
                UserRole.CUSTOMER -> CatalogScreen(
                    state = catalogState,
                    modifier = Modifier.fillMaxSize(),
                    onSearchQueryChange = catalogViewModel::onSearchQueryChange,
                    onToggleOffers = catalogViewModel::onOnlyOffersToggle,
                    onAvailabilityFilterChange = catalogViewModel::onAvailabilityFilterChange,
                    onTagToggle = catalogViewModel::onTagToggle,
                    onBookmark = catalogViewModel::onBookmark,
                    onAddToCart = catalogViewModel::onAddToCart,
                    onDecreaseCartItem = catalogViewModel::onDecreaseCartItem,
                    onRemoveFromCart = catalogViewModel::onRemoveFromCart
                )

                UserRole.EMPLOYEE -> EmployeeScreen(modifier = Modifier.fillMaxSize())
                UserRole.MANAGER -> ManagerScreen(modifier = Modifier.fillMaxSize())
                null -> Unit
            }
        }
    }
}

@Composable
private fun SessionBar(
    email: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onLogout) {
            Text("Esci")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    SmartShopMobileTheme {
        // Preview mostra il composable di login
        LoginScreen(onLogin = { _, _, _ -> })
    }
}
