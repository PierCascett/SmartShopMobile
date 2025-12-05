/**
 * EmployeeFlowUiTest.kt
 *
 * Test UI end-to-end per il flusso dipendente (picking e consegne).
 * Header KDoc aggiunto per uniformare la documentazione dei file androidTest.
 */
package it.unito.smartshopmobile.uiTest.employee

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * Test UI End-to-End per il flusso completo dell'employee.
 *
 * Questa classe verifica l'intero processo di gestione ordini da parte di un employee:
 * - Login come employee
 * - Apertura della mappa e visualizzazione ordini
 * - Selezione e presa in carico di un ordine
 * - Visualizzazione dettagli ordine
 * - Compilazione della checklist articoli
 * - Marcatura ordine come pronto al ritiro
 *
 * Include pause strategiche per permettere l'osservazione visiva di ogni step.
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 * @property employeeEmail Email di test per il login dell'employee (giulia@gmail.com)
 * @property employeePassword Password di test per il login dell'employee (casc)
 */
@RunWith(AndroidJUnit4::class)
class EmployeeFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val employeeEmail = "giulia@gmail.com"
    private val employeePassword = "casc"

    /**
     * Test completo del flusso employee: apertura mappa e gestione ordine.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Effettua il login come employee
     * 2. Clicca su "Scegli un ordine" nella mappa
     * 3. Aggiorna la lista degli ordini
     * 4. Clicca su "Prendi in carico" per il primo ordine disponibile
     * 5. Visualizza i dettagli dell'ordine
     * 6. Scorre fino alla checklist articoli
     * 7. Seleziona il toggle per il primo articolo
     * 8. Clicca su "Pronto al ritiro" per completare l'ordine
     *
     * Include pause estese per permettere l'osservazione visiva completa del flusso.
     */
    @Test
    fun employeeFlow_openMapAndChooseOrder() {
        pause(1200)
        loginAsEmployee()
        pause(1500)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Scegli un ordine", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(1000)

        composeRule.onNodeWithText("Scegli un ordine", useUnmergedTree = true)
            .performScrollTo()
        pause(800)
        composeRule.onNodeWithText("Scegli un ordine", useUnmergedTree = true)
            .performClick()
        pause(2000)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Aggiorna ordini", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Aggiorna ordini", useUnmergedTree = true)
            .onFirst()
            .performClick()
        pause(1200)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Prendi in carico", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Prendi in carico", useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
            .performClick()
        pause(1800)
        composeRule.waitForIdle()
        pause(1500)

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Checklist articoli", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(1000)

        composeRule.waitUntil(timeoutMillis = 12_000) {
            composeRule.onAllNodesWithText("Vedi", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        pause(600)
        composeRule.onAllNodesWithText("Vedi", useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
            .performClick()
        pause(1600)
        scrollDown(times = 3)

        composeRule.onAllNodesWithText("Checklist articoli", useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
        pause(1000)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(isToggleable(), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(isToggleable(), useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
            .performClick()
        pause(1400)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(
                "Pronto al ritiro",
                substring = true,
                ignoreCase = true,
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Pronto al ritiro", substring = true, ignoreCase = true, useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
            .performClick()
        pause(1800)
    }

    /**
     * Effettua il login come employee.
     *
     * Verifica se l'utente è già loggato. Se non lo è, o se è loggato con un altro account,
     * effettua il logout e poi il login con le credenziali dell'employee.
     * Attende la visualizzazione della mappa employee prima di procedere.
     */
    private fun loginAsEmployee() {
        composeRule.waitForIdle()
        pause(600)

        if (!isOnLoginScreen()) {
            val logoutNodes = composeRule.onAllNodesWithText("Esci", useUnmergedTree = true)
            if (logoutNodes.fetchSemanticsNodes().isNotEmpty()) {
                logoutNodes.onFirst().performClick()
                pause(800)
                composeRule.waitUntil(timeoutMillis = 10_000) { isOnLoginScreen() }
                pause(600)
            }
        }

        if (isOnLoginScreen()) {
            val emailField = composeRule.onAllNodes(
                hasSetTextAction() and hasImeAction(ImeAction.Next),
                useUnmergedTree = true
            )
            val passwordField = composeRule.onAllNodes(
                hasSetTextAction() and hasImeAction(ImeAction.Done),
                useUnmergedTree = true
            )
            if (emailField.fetchSemanticsNodes().isNotEmpty() && passwordField.fetchSemanticsNodes().isNotEmpty()) {
                emailField.onFirst().performTextClearance()
                pause(400)
                emailField.onFirst().performTextInput(employeeEmail)
                pause(1000)
                passwordField.onFirst().performTextClearance()
                pause(400)
                passwordField.onFirst().performTextInput(employeePassword)
                pause(1000)
                composeRule.onAllNodesWithText("Accedi", useUnmergedTree = true).onFirst().performClick()
                pause(1500)
            }
        }

        composeRule.waitUntil(timeoutMillis = 15_000) { isOnEmployeeMap() }
        pause(1000)
    }

    /**
     * Verifica se l'utente si trova nella schermata di login.
     *
     * Controlla la presenza dei campi email e password con le rispettive IME actions.
     *
     * @return true se entrambi i campi di login sono presenti, false altrimenti
     */
    private fun isOnLoginScreen(): Boolean {
        val emailField = composeRule.onAllNodes(
            hasSetTextAction() and hasImeAction(ImeAction.Next),
            useUnmergedTree = true
        )
        val passwordField = composeRule.onAllNodes(
            hasSetTextAction() and hasImeAction(ImeAction.Done),
            useUnmergedTree = true
        )
        return emailField.fetchSemanticsNodes().isNotEmpty() &&
            passwordField.fetchSemanticsNodes().isNotEmpty()
    }

    /**
     * Verifica se l'utente si trova sulla schermata della mappa employee.
     *
     * Controlla la presenza del testo "Mappa" nell'interfaccia.
     *
     * @return true se il testo "Mappa" è presente, false altrimenti
     */
    private fun isOnEmployeeMap(): Boolean {
        return composeRule.onAllNodes(
            hasText("Mappa", substring = true, ignoreCase = true),
            useUnmergedTree = true
        ).fetchSemanticsNodes().isNotEmpty()
    }

    /**
     * Scorre la schermata verso il basso (swipe up).
     *
     * Esegue un numero specificato di swipe up sulla root della UI
     * per scorrere il contenuto verso l'alto.
     *
     * @param times Numero di volte da ripetere lo scroll (default: 2)
     */
    private fun scrollDown(times: Int = 2) {
        repeat(times) {
            composeRule.onRoot().performTouchInput { swipeUp() }
            pause(400)
        }
    }

    /**
     * Utility per rallentare visivamente i passaggi del test.
     *
     * Mette in pausa l'esecuzione per permettere l'osservazione visiva
     * del comportamento dell'interfaccia utente durante il test.
     *
     * @param ms Durata della pausa in millisecondi (default: 1200ms)
     */
    private fun pause(ms: Long = 1200) {
        Thread.sleep(ms)
    }
}
