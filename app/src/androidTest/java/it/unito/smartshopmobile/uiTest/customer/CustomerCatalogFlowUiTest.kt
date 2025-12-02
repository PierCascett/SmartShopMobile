package it.unito.smartshopmobile.uiTest.customer

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
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
 * UI test E2E per il flusso del cliente:
 * - Login con credenziali casc@gmail.com / casc
 * - Navigazione nel catalogo
 * - Selezione tag "Offerte attive"
 * - Selezione del secondo prodotto (detersivo per piatti limone)
 */
@RunWith(AndroidJUnit4::class)
class CustomerCatalogFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    private val customerEmail = "casc@gmail.com"
    private val customerPassword = "casc"

    @Test
    fun customerFlow_selectActiveOffers_andSelectDishwashingProduct() {
        pause(1200)
        performLoginIfNeeded()
        pause(1200)
        ensureOffersFilterEnabled()
        pause(1200)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(
                "detersivo",
                substring = true,
                ignoreCase = true,
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        pause(1200)

        // Seleziona il prodotto in due step (scroll poi click) per rendere l'interazione visibile
        val productMatcher = hasText("limone", substring = true, ignoreCase = true) or
                hasText("piatti", substring = true, ignoreCase = true)
        composeRule.onNode(productMatcher, useUnmergedTree = true).performScrollTo()
        pause(1000)
        composeRule.onNode(productMatcher, useUnmergedTree = true).performClick()

        composeRule.waitForIdle()
        pause(1800)
    }

    @Test
    fun customerFlow_navigateThroughCatalogTabs() {
        pause(800)
        performLoginIfNeeded()
        pause(1200)

        composeRule.waitUntil(timeoutMillis = 15_000) { isCatalogVisible() }
        composeRule.onNodeWithText("Catalogo", useUnmergedTree = true).assertExists()
        pause(1000)

        ensureOffersFilterEnabled()
        pause(1000)
        composeRule.onNodeWithText("Offerte attive", useUnmergedTree = true).assertExists()
        pause(800)

        composeRule.onNodeWithText("Offerte attive", useUnmergedTree = true).performClick()
        pause(1200)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(1200)
    }

    @Test
    fun customerFlow_addProductToCart() {
        pause(800)
        performLoginIfNeeded()
        pause(1000)
        ensureOffersFilterEnabled()
        pause(1000)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(
                hasText("detersivo", substring = true, ignoreCase = true) or
                        hasText("limone", substring = true, ignoreCase = true),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        pause(1000)

        val productMatcher = hasText("detersivo", substring = true, ignoreCase = true) or
                hasText("limone", substring = true, ignoreCase = true)
        composeRule.onNode(productMatcher, useUnmergedTree = true).performScrollTo()
        pause(1000)
        composeRule.onNode(productMatcher, useUnmergedTree = true).performClick()
        pause(1500)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Aggiungi", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(800)
        composeRule.onAllNodesWithText("Aggiungi", useUnmergedTree = true).onFirst().performClick()
        composeRule.waitForIdle()
        pause(2000) // Pausa più lunga per vedere l'aggiunta al carrello

        openCartOverlay()
        pause(1500) // Pausa dopo apertura carrello per vedere bene

        // Verifica che il carrello sia aperto e contenga prodotti
        composeRule.onNodeWithText("Carrello", useUnmergedTree = true).assertExists()
        pause(1000)

        // Attendi che il pulsante + sia visibile nel carrello
        composeRule.waitUntil(timeoutMillis = 15_000) {
            val nodes = composeRule.onAllNodesWithContentDescription("Aumenta quantita'", useUnmergedTree = true)
                .fetchSemanticsNodes()
            println("DEBUG: Trovati ${nodes.size} nodi con contentDescription 'Aumenta quantita'")
            nodes.isNotEmpty()
        }
        pause(1000)

        // Fai 2 click sul pulsante + per aumentare la quantità
        println("DEBUG: Inizio click sul pulsante +")
        repeat(2) { index ->
            println("DEBUG: Click ${index + 1}/2")
            composeRule.onAllNodesWithContentDescription("Aumenta quantita'", useUnmergedTree = true)
                .onLast().performClick() // Uso onLast() per prendere quello nel carrello
            pause(1000) // Pausa più lunga tra i click per vedere l'incremento
        }
        pause(1500) // Pausa finale per vedere la quantità aggiornata

        composeRule.onNodeWithText("Procedi all'ordine", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        pause(800)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Ordine #", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(800)
    }

    private fun performLoginIfNeeded() {
        composeRule.waitForIdle()
        pause(600)
        if (isCatalogVisible()) return

        val emailField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Companion.Next), useUnmergedTree = true)
        val passwordField =
            composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Companion.Done), useUnmergedTree = true)

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

    private fun ensureOffersFilterEnabled() {
        composeRule.waitUntil(timeoutMillis = 15_000) { isCatalogVisible() }
        pause(600)
        val showOffersChip =
            composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
        val activeOffersChip =
            composeRule.onAllNodesWithText("Offerte attive", useUnmergedTree = true)

        when {
            showOffersChip.fetchSemanticsNodes().isNotEmpty() -> {
                showOffersChip.onFirst().performClick()
                pause(1000)
            }
            activeOffersChip.fetchSemanticsNodes().isNotEmpty() -> Unit // Già attivo
            else -> throw AssertionError("Chip offerte non presente")
        }
        composeRule.waitForIdle()
        pause(600)
    }

    private fun isCatalogVisible(): Boolean {
        val offers = composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
        val activeOffers = composeRule.onAllNodesWithText("Offerte attive", useUnmergedTree = true)
        return offers.fetchSemanticsNodes().isNotEmpty() || activeOffers.fetchSemanticsNodes().isNotEmpty()
    }

    private fun openCartOverlay() {
        composeRule.waitUntil(timeoutMillis = 10_000) { isCatalogVisible() }
        composeRule.onNodeWithContentDescription("Apri carrello", useUnmergedTree = true).performClick()
        pause(800)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Carrello", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(600)
    }

    // Piccola utility per rallentare visivamente i passaggi
    private fun pause(ms: Long = 1200) {
        Thread.sleep(ms)
    }
}
