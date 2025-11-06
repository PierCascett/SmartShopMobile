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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.unito.smartshopmobile.ui.screens.LoginScreenMVVM
import it.unito.smartshopmobile.ui.screens.LoginScreen
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.LoginViewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartShopMobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreenMVVM(
                        viewModel = loginViewModel,
                        modifier = Modifier.padding(innerPadding),
                        onLoginSuccess = { email ->
                            Toast.makeText(this, "Accesso: $email", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    SmartShopMobileTheme {
        // Preview mostra il composable di login
        LoginScreen(onLogin = { _, _ -> })
    }
}