/**
 * CustomerHomeDeliveryUiTest.kt
 *
 * Test UI end-to-end per la consegna a domicilio del cliente.
 * Header aggiunto per mantenere documentazione uniforme nel modulo androidTest.
 */
package it.unito.smartshopmobile.uiTest.customer

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unito.smartshopmobile.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test UI per il flusso di consegna a domicilio del cliente.
 *
 * Questa classe verifica il processo completo di:
 * - Selezione di un prodotto dal catalogo
 * - Aggiunta del prodotto al carrello
 * - Impostazione della modalità di consegna a domicilio
 * - Invio dell'ordine
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property customerEmail Email di test per il login del cliente
 * @property customerPassword Password di test per il login del cliente
 */
@RunWith(AndroidJUnit4::class)
class CustomerHomeDeliveryUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val customerEmail = "casc@gmail.com"
    private val customerPassword = "casc"

    /**
     * Test che verifica la capacità del cliente di impostare la consegna a domicilio
     * e inviare un ordine.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come cliente se necessario
     * 2. Abilita il filtro offerte
     * 3. Cerca e seleziona un prodotto "detersivo"
     * 4. Aggiunge il prodotto al carrello
     * 5. Apre l'overlay del carrello
     * 6. Imposta la modalità di consegna "Domicilio"
     * 7. Procede con l'invio dell'ordine
     *
     * Include pause strategiche per permettere l'osservazione visiva.
     */
    @Test
    fun customer_can_set_home_delivery_and_submit_order() {
        pause(800)
        performLoginIfNeeded()
        pause(1200)
        ensureOffersFilterEnabled()
        pause(1000)

        // Seleziona un prodotto e lo aggiunge al carrello
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(
                "detersivo",
                substring = true,
                ignoreCase = true,
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        pause(800)

        val productMatcher = composeRule.onNodeWithText("detersivo", substring = true, ignoreCase = true)
        productMatcher.performScrollTo()
        pause(800)
        productMatcher.performClick()
        pause(1200)

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Aggiungi", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Aggiungi", useUnmergedTree = true)
            .onFirst()
            .performClick()
        pause(1500)

        openCartOverlay()
        pause(1000)

        // Imposta consegna a domicilio e prova a inviare l'ordine
        composeRule.onAllNodesWithText("Domicilio", useUnmergedTree = true)
            .onFirst()
            .performClick()
        pause(1000)

        composeRule.onNodeWithText("Procedi all'ordine", useUnmergedTree = true)
            .performClick()
        pause(4000)
    }

    /**
     * Effettua il login se necessario.
     *
     * Controlla se i campi di login sono presenti. Se sì, li compila
     * con le credenziali del cliente ed esegue il login, poi attende
     * la visualizzazione del catalogo.
     */
    private fun performLoginIfNeeded() {
        composeRule.waitForIdle()
        pause(600)
        val emailField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Next), useUnmergedTree = true)
        val passwordField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done), useUnmergedTree = true)

        if (emailField.fetchSemanticsNodes().isNotEmpty() && passwordField.fetchSemanticsNodes().isNotEmpty()) {
            emailField.onFirst().performTextClearance()
            pause(300)
            emailField.onFirst().performTextInput(customerEmail)
            pause(600)
            passwordField.onFirst().performTextClearance()
            pause(300)
            passwordField.onFirst().performTextInput(customerPassword)
            pause(600)
            composeRule.onAllNodesWithText("Accedi", useUnmergedTree = true).onFirst().performClick()
            pause(1400)
        }

        composeRule.waitUntil(timeoutMillis = 15_000) { isCatalogVisible() }
        pause(600)
    }

    /**
     * Assicura che il filtro offerte sia abilitato.
     *
     * Attende la visualizzazione del catalogo e verifica lo stato del chip offerte.
     * Se il chip "Mostra offerte" è presente, lo clicca per attivarlo.
     */
    private fun ensureOffersFilterEnabled() {
        composeRule.waitUntil(timeoutMillis = 15_000) { isCatalogVisible() }
        val showOffersChip = composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
        val activeOffersChip = composeRule.onAllNodesWithText("Offerte attive", useUnmergedTree = true)

        when {
            showOffersChip.fetchSemanticsNodes().isNotEmpty() -> {
                showOffersChip.onFirst().performClick()
                pause(800)
            }
            activeOffersChip.fetchSemanticsNodes().isNotEmpty() -> Unit
        }
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
     * Apre l'overlay del carrello.
     *
     * Attende che il catalogo sia visibile, poi clicca sull'icona del carrello
     * e attende che l'overlay si apra completamente.
     */
    private fun openCartOverlay() {
        composeRule.waitUntil(timeoutMillis = 10_000) { isCatalogVisible() }
        composeRule.onNodeWithContentDescription("Apri carrello", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Carrello", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
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
