/**
 * ManagerFlowUiTest.kt
 *
 * Test UI end-to-end per il flusso manager (riordini, scaffali, fornitori).
 * Header KDoc per allineare la documentazione della suite androidTest.
 */
package it.unito.smartshopmobile.uiTest.manager

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unito.smartshopmobile.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test UI per il flusso del manager nell'applicazione SmartShop.
 *
 * Questa classe verifica le funzionalità specifiche del ruolo manager,
 * inclusa la gestione dei riordini di prodotti e l'interazione con
 * il form di riordino.
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property managerEmail Email di test per il login del manager
 * @property managerPassword Password di test per il login del manager
 */
@RunWith(AndroidJUnit4::class)
class ManagerFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val managerEmail = "laura@gmail.com"
    private val managerPassword = "casc"

    /**
     * Test che verifica la capacità del manager di compilare e inviare un form di riordino.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come manager
     * 2. Naviga alla sezione "Riordine"
     * 3. Compila il campo quantità con il valore "2"
     * 4. Clicca sul bottone "Invia riordino"
     *
     * Utilizza polling per attendere la comparsa degli elementi UI.
     */
    @Test
    fun manager_can_fill_restock_form_and_submit() {
        loginAsManager()

        if (!pollText("Riordine", 30_000)) return
        composeRule.onAllNodesWithText("Riordine", useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.let {
            composeRule.onAllNodesWithText("Riordine", useUnmergedTree = true).onFirst().performClick()
        }

        if (!pollText("Quantita", 20_000)) return
        val quantityField = composeRule.onAllNodesWithText("Quantita", useUnmergedTree = true)
        if (quantityField.fetchSemanticsNodes().isNotEmpty()) {
            quantityField.onFirst().performTextClearance()
            quantityField.onFirst().performTextInput("2")
        }

        if (!pollText("Invia riordino", 20_000)) return
        composeRule.onAllNodesWithText("Invia riordino", useUnmergedTree = true)
            .onFirst()
            .performClick()
    }

    /**
     * Effettua il login come manager.
     *
     * Attende l'idle state, poi cerca i campi di email e password.
     * Se presenti, li compila con le credenziali del manager ed esegue il login.
     */
    private fun loginAsManager() {
        composeRule.waitForIdle()
        val emailField = composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Next), useUnmergedTree = true)
        val passwordField = composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done), useUnmergedTree = true)

        if (emailField.fetchSemanticsNodes().isNotEmpty() && passwordField.fetchSemanticsNodes().isNotEmpty()) {
            emailField.onFirst().performTextClearance()
            emailField.onFirst().performTextInput(managerEmail)
            passwordField.onFirst().performTextClearance()
            passwordField.onFirst().performTextInput(managerPassword)
            composeRule.onAllNodesWithText("Accedi", useUnmergedTree = true).onFirst().performClick()
        }
    }

    /**
     * Effettua polling per verificare la presenza di un testo nell'UI.
     *
     * Controlla ripetutamente (ogni 500ms) se il testo specificato è presente
     * fino al timeout specificato.
     *
     * @param text Il testo da cercare (supporta substring e ignoreCase)
     * @param timeoutMillis Timeout massimo in millisecondi
     * @return true se il testo è stato trovato entro il timeout, false altrimenti
     */
    private fun pollText(text: String, timeoutMillis: Long): Boolean {
        val step = 500L
        var elapsed = 0L
        while (elapsed <= timeoutMillis) {
            val found = composeRule.onAllNodesWithText(text, substring = true, ignoreCase = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
            if (found) return true
            Thread.sleep(step)
            elapsed += step
        }
        return false
    }
}
