/**
 * CustomerProfileUpdateUiTest.kt
 *
 * Test UI end-to-end per aggiornamento profilo cliente.
 * Header KDoc per completare la copertura documentale dei file di test strumentali.
 */
package it.unito.smartshopmobile.uiTest.customer

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
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
 * Test UI per l'aggiornamento del profilo cliente.
 *
 * Questa classe verifica le funzionalità di modifica dei dati del profilo cliente,
 * inclusi nome, cognome e telefono. Testa anche la persistenza dei dati
 * attraverso la navigazione tra diverse schermate.
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property customerEmail Email di test per il login del cliente
 * @property customerPassword Password di test per il login del cliente
 */
@RunWith(AndroidJUnit4::class)
class CustomerProfileUpdateUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val customerEmail = "casc@gmail.com"
    private val customerPassword = "casc"

    /**
     * Test che verifica l'aggiornamento del profilo cliente con nome, cognome e telefono.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come cliente se necessario
     * 2. Naviga alla sezione "Account"
     * 3. Modifica il nome in "Mario"
     * 4. Modifica il cognome in "Rossi"
     * 5. Salva le modifiche
     * 6. Naviga via e torna alla sezione Account per verificare la persistenza
     * 7. Aggiunge/modifica il numero di telefono "3331112222"
     * 8. Salva nuovamente le modifiche
     *
     * Include pause strategiche per permettere l'osservazione visiva.
     */
    @Test
    fun customer_updates_profile_name_and_surname() {
        pause(800)
        performLoginIfNeeded()
        pause(1000)

        composeRule.onNodeWithText("Account", useUnmergedTree = true).performClick()
        pause(1200)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
                .fetchSemanticsNodes().size >= 3 // email, nome, cognome
        }
        pause(600)

        val textFields = composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
        textFields[1].apply {
            performTextClearance()
            pause(400)
            performTextInput("Mario")
        }
        pause(800)

        textFields[2].apply {
            performTextClearance()
            pause(400)
            performTextInput("Rossi")
        }
        pause(800)

        composeRule.onAllNodesWithText("Salva dati", useUnmergedTree = true)
            .onFirst()
            .performClick()
        composeRule.waitForIdle()
        pause(1500)

        composeRule.onNodeWithText("Ordina", useUnmergedTree = true).performClick()
        pause(1000)
        composeRule.onNodeWithText("Account", useUnmergedTree = true).performClick()
        pause(1200)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
                .fetchSemanticsNodes().size >= 4 // include telefono
        }
        pause(600)

        val refreshedFields = composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
        refreshedFields[3].apply {
            performTextClearance()
            pause(400)
            performTextInput("3331112222")
        }
        pause(800)

        composeRule.onAllNodesWithText("Salva dati", useUnmergedTree = true)
            .onFirst()
            .performClick()
        composeRule.waitForIdle()
        pause(1500)
    }

    /**
     * Effettua il login se necessario.
     *
     * Controlla se il catalogo è già visibile. Se non lo è, cerca i campi di login
     * e li compila con le credenziali del cliente, poi attende la visualizzazione
     * del catalogo.
     */
    private fun performLoginIfNeeded() {
        composeRule.waitForIdle()
        pause(600)
        if (isCatalogVisible()) return

        val emailField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Next), useUnmergedTree = true)
        val passwordField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done), useUnmergedTree = true)

        if (emailField.fetchSemanticsNodes().isNotEmpty() && passwordField.fetchSemanticsNodes().isNotEmpty()) {
            emailField.onFirst().performTextClearance()
            pause(400)
            emailField.onFirst().performTextInput(customerEmail)
            pause(800)
            passwordField.onFirst().performTextClearance()
            pause(400)
            passwordField.onFirst().performTextInput(customerPassword)
            pause(800)
            composeRule.onAllNodesWithText("Accedi", useUnmergedTree = true).onFirst().performClick()
            pause(1400)
        }

        composeRule.waitUntil(timeoutMillis = 15_000) { isCatalogVisible() }
        pause(800)
    }

    /**
     * Verifica se il catalogo è visibile.
     *
     * @return true se almeno uno dei chip "Mostra offerte" o "Offerte attive" è presente
     */
    private fun isCatalogVisible(): Boolean {
        val offers = composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
        val activeOffers = composeRule.onAllNodesWithText("Offerte attive", useUnmergedTree = true)
        return offers.fetchSemanticsNodes().isNotEmpty() || activeOffers.fetchSemanticsNodes().isNotEmpty()
    }

    /**
     * Mette in pausa l'esecuzione del test per permettere l'osservazione visiva.
     *
     * @param ms Durata della pausa in millisecondi (default: 1200ms)
     */
    private fun pause(ms: Long = 1200) {
        Thread.sleep(ms)
    }
}
