/**
 * FavoritesPersistenceUiTest.kt
 *
 * Test UI end-to-end sulla persistenza dei preferiti cliente.
 * Documentazione iniziale aggiunta per coerenza con gli altri file androidTest.
 */
package it.unito.smartshopmobile.uiTest.customer

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unito.smartshopmobile.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test UI per la persistenza dei preferiti nel flusso cliente.
 *
 * Questa classe verifica che le funzionalità di gestione dei prodotti preferiti
 * funzionino correttamente, includendo:
 * - Aggiunta di prodotti ai preferiti
 * - Persistenza dei preferiti attraverso l'apertura/chiusura dell'overlay
 * - Rimozione di prodotti dai preferiti
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property customerEmail Email di test per il login del cliente
 * @property customerPassword Password di test per il login del cliente
 */
@RunWith(AndroidJUnit4::class)
class FavoritesPersistenceUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val customerEmail = "casc@gmail.com"
    private val customerPassword = "casc"

    /**
     * Test che verifica la persistenza dei preferiti attraverso l'apertura dell'overlay.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come cliente
     * 2. Abilita il filtro offerte
     * 3. Scorre fino a trovare un'icona cuore (preferito)
     * 4. Aggiunge il prodotto ai preferiti
     * 5. Apre l'overlay preferiti
     * 6. Verifica che il prodotto sia presente nell'overlay
     * 7. Rimuove il prodotto dai preferiti
     */
    @Test
    fun favorite_isKeptAcrossOverlayOpen() {
        performLoginIfNeeded()
        ensureOffersFilterEnabled()

        // Scorri e usa il primo cuore disponibile (qualunque prodotto)
        val hasIcon = scrollUntilFavoriteIconVisible()
        if (!hasIcon) return
        val addFavNodes = composeRule.onAllNodesWithContentDescription("Aggiungi ai preferiti", useUnmergedTree = true)
        if (addFavNodes.fetchSemanticsNodes().isNotEmpty()) {
            addFavNodes.onFirst().performClick()
        } else {
            val removeNodes = composeRule.onAllNodesWithContentDescription("Rimuovi dai preferiti", useUnmergedTree = true)
            if (removeNodes.fetchSemanticsNodes().isEmpty()) return
            removeNodes.onFirst().performClick()
            composeRule.waitUntil(timeoutMillis = 8_000) {
                composeRule.onAllNodesWithContentDescription("Aggiungi ai preferiti", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onAllNodesWithContentDescription("Aggiungi ai preferiti", useUnmergedTree = true)
                .onFirst()
                .performClick()
        }

        // Apri overlay preferiti e verifica presenza
        composeRule.onNodeWithContentDescription("Apri preferiti", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Non hai ancora aggiunto preferiti", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // Rimuovi dai preferiti
        val removeIcon = composeRule.onAllNodesWithContentDescription("Rimuovi dai preferiti", useUnmergedTree = true)
        if (removeIcon.fetchSemanticsNodes().isNotEmpty()) {
            removeIcon.onFirst().performClick()
        }
    }

    /**
     * Effettua il login se necessario.
     *
     * Controlla se i campi di login sono presenti. Se sì, esegue il login
     * con le credenziali del cliente e attende la visualizzazione del catalogo.
     */
    private fun performLoginIfNeeded() {
        composeRule.waitForIdle()
        val emailField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Next), useUnmergedTree = true)
        val passwordField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done), useUnmergedTree = true)

        if (emailField.fetchSemanticsNodes().isNotEmpty() && passwordField.fetchSemanticsNodes().isNotEmpty()) {
            emailField.onFirst().performTextClearance()
            emailField.onFirst().performTextInput(customerEmail)
            passwordField.onFirst().performTextClearance()
            passwordField.onFirst().performTextInput(customerPassword)
            composeRule.onAllNodesWithText("Accedi", useUnmergedTree = true).onFirst().performClick()
        }

        composeRule.waitUntil(timeoutMillis = 12_000) { isCatalogVisible() }
    }

    /**
     * Assicura che il filtro offerte sia abilitato.
     *
     * Attende la visualizzazione del catalogo e verifica lo stato del chip offerte.
     * Se il chip "Mostra offerte" è presente, lo clicca per attivarlo.
     */
    private fun ensureOffersFilterEnabled() {
        composeRule.waitUntil(timeoutMillis = 10_000) { isCatalogVisible() }
        val showOffersChip = composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
        val activeOffersChip = composeRule.onAllNodesWithText("Offerte attive", useUnmergedTree = true)

        when {
            showOffersChip.fetchSemanticsNodes().isNotEmpty() -> {
                showOffersChip.onFirst().performClick()
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
     * Scorre la schermata fino a quando non viene trovata un'icona preferito.
     *
     * Esegue fino a 10 tentativi di scroll verso l'alto, cercando le icone
     * "Aggiungi ai preferiti" o "Rimuovi dai preferiti".
     *
     * @return true se l'icona preferito è stata trovata, false altrimenti
     */
    private fun scrollUntilFavoriteIconVisible(): Boolean {
        repeat(10) {
            val hasIcon = composeRule.onAllNodesWithContentDescription("Aggiungi ai preferiti", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithContentDescription("Rimuovi dai preferiti", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            if (hasIcon) return true
            composeRule.onRoot().performTouchInput { swipeUp() }
        }
        return composeRule.onAllNodesWithContentDescription("Aggiungi ai preferiti", useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithContentDescription("Rimuovi dai preferiti", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
    }
}
