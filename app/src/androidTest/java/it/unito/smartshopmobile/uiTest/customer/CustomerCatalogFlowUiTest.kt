/**
 * CustomerCatalogFlowUiTest.kt
 *
 * Test UI end-to-end per il catalogo clienti (androidTest).
 * Introdotto header KDoc per allineare la documentazione dei file della suite.
 */
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
 * Test UI End-to-End per il flusso di navigazione del catalogo cliente.
 *
 * Questa classe verifica le funzionalità del catalogo prodotti per il cliente:
 * - Login con credenziali di test
 * - Navigazione nel catalogo
 * - Selezione e applicazione di filtri (Offerte attive)
 * - Selezione di prodotti specifici
 * - Aggiunta prodotti al carrello e gestione quantità
 * - Invio ordini
 *
 * Include pause strategiche per permettere l'osservazione visiva di ogni step.
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property customerEmail Email di test per il login del cliente (casc@gmail.com)
 * @property customerPassword Password di test per il login del cliente (casc)
 */
@RunWith(AndroidJUnit4::class)
class CustomerCatalogFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    private val customerEmail = "casc@gmail.com"
    private val customerPassword = "casc"

    /**
     * Test che verifica la selezione di prodotti con il filtro "Offerte attive".
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come cliente se necessario
     * 2. Abilita il filtro "Offerte attive"
     * 3. Attende la visualizzazione di prodotti con "detersivo" nel nome
     * 4. Scorre fino al prodotto specifico (limone/piatti)
     * 5. Seleziona il prodotto
     */
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

    /**
     * Test che verifica la navigazione tra le tab del catalogo e i filtri.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come cliente se necessario
     * 2. Verifica la presenza della tab "Catalogo"
     * 3. Abilita il filtro "Offerte attive"
     * 4. Clicca nuovamente sul chip per disattivarlo
     * 5. Verifica che ritorni a "Mostra offerte"
     */
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

    /**
     * Test completo che verifica l'aggiunta di un prodotto al carrello e l'invio dell'ordine.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come cliente se necessario
     * 2. Abilita il filtro offerte
     * 3. Cerca e seleziona un prodotto (detersivo/limone)
     * 4. Aggiunge il prodotto al carrello
     * 5. Apre l'overlay del carrello
     * 6. Aumenta la quantità del prodotto di 2 unità usando il pulsante +
     * 7. Procede con l'invio dell'ordine
     * 8. Verifica che l'ordine sia stato creato (Ordine #)
     *
     * Include pause estese per permettere l'osservazione visiva di ogni azione.
     */
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

    /**
     * Effettua il login se necessario.
     *
     * Controlla se il catalogo è già visibile. Se non lo è, cerca i campi di login,
     * li compila con le credenziali del cliente ed esegue il login.
     * Attende poi la visualizzazione del catalogo.
     */
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

    /**
     * Assicura che il filtro offerte sia abilitato.
     *
     * Attende la visualizzazione del catalogo e verifica lo stato del chip offerte.
     * Se il chip "Mostra offerte" è presente, lo clicca per attivarlo.
     * Se il chip "Offerte attive" è presente, significa che è già attivo.
     *
     * @throws AssertionError se nessun chip offerte è presente
     */
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
        pause(800)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Carrello", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(600)
    }

    /**
     * Utility per rallentare visivamente i passaggi del test.
     *
     * Mette in pausa l'esecuzione per permettere l'osservazione visiva
     * del comportamento dell'interfaccia utente.
     *
     * @param ms Durata della pausa in millisecondi (default: 1200ms)
     */
    private fun pause(ms: Long = 1200) {
        Thread.sleep(ms)
    }
}
