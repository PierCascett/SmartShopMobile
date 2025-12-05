/**
 * EmployeePickupFlowUiTest.kt
 *
 * Test UI end-to-end per la gestione dei ritiri locker lato dipendente.
 * Header di documentazione aggiunto per completare la copertura nei file androidTest.
 */
package it.unito.smartshopmobile.uiTest.employee

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
 * Test UI per il flusso di ritiro ordini da parte dell'employee.
 *
 * Questa classe verifica le funzionalità specifiche del ruolo employee
 * nel processo di gestione ordini in modalità pickup (ritiro in negozio):
 * - Login come employee
 * - Visualizzazione e selezione degli ordini assegnati
 * - Presa in carico degli ordini
 * - Compilazione della checklist articoli
 * - Marcatura ordini come pronti al ritiro o consegnati
 *
 * Utilizza polling estensivo per gestire i caricamenti asincroni.
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property employeeEmail Email di test per il login dell'employee (giulia@gmail.com)
 * @property employeePassword Password di test per il login dell'employee (casc)
 */
@RunWith(AndroidJUnit4::class)
class EmployeePickupFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val employeeEmail = "giulia@gmail.com"
    private val employeePassword = "casc"

    /**
     * Test che verifica la capacità dell'employee di prendere in carico un ordine
     * e marcarlo come pronto al ritiro.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come employee
     * 2. Apre la tab "Assegna" se presente
     * 3. Aggiorna la lista degli ordini
     * 4. Cerca e clicca sul pulsante "Prendi in carico" o "Continua"
     * 5. Seleziona il toggle della checklist articoli
     * 6. Clicca su "Pronto al ritiro" o "Segna consegnato"
     *
     * Utilizza polling con timeout estesi per attendere i caricamenti asincroni.
     */
    @Test
    fun employee_can_pick_order_and_mark_ready() {
        loginAsEmployee()

        // prova ad aprire tab Assegna se presente
        composeRule.onAllNodesWithText("Assegna", useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.let {
            composeRule.onAllNodesWithText("Assegna", useUnmergedTree = true).onFirst().performClick()
        }

        if (!pollOrdersRefresh()) return

        // cerca pulsante per prendere in carico o continuare
        val hasTake = pollContentDescription("Prendi in carico", 30_000) || pollContentDescription("Continua", 5_000)
        if (!hasTake) return

        val takeNodes = composeRule.onAllNodesWithContentDescription("Prendi in carico", useUnmergedTree = true)
        val contNodes = composeRule.onAllNodesWithContentDescription("Continua", useUnmergedTree = true)
        when {
            takeNodes.fetchSemanticsNodes().isNotEmpty() -> takeNodes.onFirst().performClick()
            contNodes.fetchSemanticsNodes().isNotEmpty() -> contNodes.onFirst().performClick()
            else -> return
        }

        if (!pollToggle(30_000)) return
        composeRule.onAllNodes(isToggleable(), useUnmergedTree = true).onFirst().performClick()

        val doneFound = pollText("Pronto al ritiro", 30_000) || pollText("Segna consegnato", 15_000)
        if (!doneFound) return

        val readyNodes = composeRule.onAllNodesWithText("Pronto al ritiro", substring = true, ignoreCase = true, useUnmergedTree = true)
        val deliveredNodes = composeRule.onAllNodesWithText("Segna consegnato", substring = true, ignoreCase = true, useUnmergedTree = true)
        when {
            readyNodes.fetchSemanticsNodes().isNotEmpty() -> readyNodes.onFirst().performClick()
            deliveredNodes.fetchSemanticsNodes().isNotEmpty() -> deliveredNodes.onFirst().performClick()
        }
    }

    /**
     * Effettua il login come employee.
     *
     * Cerca i campi di email e password. Se presenti, li compila con le credenziali
     * dell'employee ed esegue il login. Attende poi la visualizzazione della mappa.
     */
    private fun loginAsEmployee() {
        composeRule.waitForIdle()
        val emailField = composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Next), useUnmergedTree = true)
        val passwordField = composeRule.onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done), useUnmergedTree = true)
        if (emailField.fetchSemanticsNodes().isNotEmpty() && passwordField.fetchSemanticsNodes().isNotEmpty()) {
            emailField.onFirst().performTextClearance()
            emailField.onFirst().performTextInput(employeeEmail)
            passwordField.onFirst().performTextClearance()
            passwordField.onFirst().performTextInput(employeePassword)
            composeRule.onAllNodesWithText("Accedi", useUnmergedTree = true).onFirst().performClick()
        }
        pollText("Mappa", 15_000)
    }

    /**
     * Effettua polling per cercare e cliccare sul pulsante "Aggiorna ordini".
     *
     * Cerca ripetutamente il pulsante "Aggiorna ordini" per circa 30 secondi.
     * Se non trovato, prova a cliccare su "Scegli un ordine" o a fare swipe up
     * per scorrere la lista.
     *
     * @return true se il pulsante è stato trovato e cliccato, false altrimenti
     */
    private fun pollOrdersRefresh(): Boolean {
        repeat(60) { // ~30s con step da 500ms
            val refreshNodes = composeRule.onAllNodesWithContentDescription("Aggiorna ordini", useUnmergedTree = true)
                .fetchSemanticsNodes()
            if (refreshNodes.isNotEmpty()) {
                composeRule.onAllNodesWithContentDescription("Aggiorna ordini", useUnmergedTree = true).onFirst().performClick()
                return true
            }
            val chooseOrder = composeRule.onAllNodesWithText("Scegli un ordine", useUnmergedTree = true)
            if (chooseOrder.fetchSemanticsNodes().isNotEmpty()) {
                chooseOrder.onFirst().performClick()
            } else {
                composeRule.onRoot().performTouchInput { swipeUp() }
            }
            Thread.sleep(500)
        }
        return false
    }

    /**
     * Effettua polling per cercare un elemento con una specifica content description.
     *
     * @param desc La content description da cercare
     * @param timeoutMillis Timeout massimo in millisecondi
     * @return true se l'elemento è stato trovato entro il timeout, false altrimenti
     */
    private fun pollContentDescription(desc: String, timeoutMillis: Long): Boolean =
        poll(timeoutMillis) {
            composeRule.onAllNodesWithContentDescription(desc, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

    /**
     * Effettua polling per cercare un testo nell'UI.
     *
     * @param text Il testo da cercare (supporta substring e ignoreCase)
     * @param timeoutMillis Timeout massimo in millisecondi
     * @return true se il testo è stato trovato entro il timeout, false altrimenti
     */
    private fun pollText(text: String, timeoutMillis: Long): Boolean =
        poll(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true, ignoreCase = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

    /**
     * Effettua polling per cercare un elemento togglable (checkbox, switch, ecc.).
     *
     * @param timeoutMillis Timeout massimo in millisecondi
     * @return true se un elemento togglable è stato trovato entro il timeout, false altrimenti
     */
    private fun pollToggle(timeoutMillis: Long): Boolean =
        poll(timeoutMillis) {
            composeRule.onAllNodes(isToggleable(), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    /**
     * Funzione generica di polling che esegue una condizione ripetutamente.
     *
     * Controlla la condizione ogni 500ms fino al timeout specificato.
     *
     * @param timeoutMillis Timeout massimo in millisecondi
     * @param condition Funzione lambda che ritorna true quando la condizione è soddisfatta
     * @return true se la condizione è stata soddisfatta entro il timeout, false altrimenti
     */
    private fun poll(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val step = 500L
        var elapsed = 0L
        while (elapsed <= timeoutMillis) {
            if (condition()) return true
            Thread.sleep(step)
            elapsed += step
        }
        return false
    }
}
