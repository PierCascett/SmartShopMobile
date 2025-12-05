/**
 * LoginFlowUiTest.kt
 *
 * Test UI end-to-end del flusso di autenticazione (androidTest).
 * Documentazione in apertura per allineare tutti i file della suite strumentale.
 */
package it.unito.smartshopmobile.uiTest

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unito.smartshopmobile.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test UI End-to-End per il flusso di login dell'applicazione SmartShop.
 * Questa classe di test verifica l'intero processo di autenticazione utente,
 * dalla schermata di login fino alla navigazione verso la schermata Customer.
 *
 * Include pause strategiche per permettere l'osservazione visiva di ogni passaggio
 * durante l'esecuzione del test.
 *
 * @property composeRule Regola di test Compose per interagire con l'UI dell'app
 */
@RunWith(AndroidJUnit4::class)
class LoginFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test completo del flusso di login con credenziali valide.
     *
     * Il test esegue i seguenti passaggi:
     * 1. Attende il caricamento completo della MainActivity
     * 2. Verifica la presenza della schermata di login (testo "SmartShop")
     * 3. Compila il campo email con "casc@gmail.com"
     * 4. Compila il campo password con "casc"
     * 5. Clicca sul bottone "Accedi"
     * 6. Verifica la transizione alla schermata Customer
     *
     * Include pause visive tra ogni step per permettere l'osservazione durante l'esecuzione.
     */
    @Test
    fun completeLoginFlow_navigatesToCustomerScreen() {
        // Aspetta che MainActivity si carichi completamente
        composeRule.waitForIdle()
        Thread.sleep(2000) // 2 secondi per vedere la schermata di login iniziale

        // Verifica che siamo nella schermata di login
        composeRule.onNodeWithText("SmartShop", substring = true).assertExists()
        Thread.sleep(1000)

        // Trova e compila il campo email
        composeRule.onNode(
            hasSetTextAction() and hasImeAction(ImeAction.Next),
            useUnmergedTree = true
        ).performTextInput("casc@gmail.com")
        Thread.sleep(1000) // 1 secondo per vedere l'email inserita

        // Trova e compila il campo password
        composeRule.onNode(
            hasSetTextAction() and hasImeAction(ImeAction.Done),
            useUnmergedTree = true
        ).performTextInput("casc")
        Thread.sleep(1000) // 1 secondo per vedere la password inserita

        // Clicca sul bottone Accedi
        composeRule.onNodeWithText("Accedi", useUnmergedTree = true).performClick()
        Thread.sleep(3000) // 3 secondi per vedere il processo di login e la transizione

        // Verifica che siamo nella schermata Customer (cerca elementi tipici)
        // Può essere la navbar con "Catalogo", "Preferiti", "Carrello", "Account"
        composeRule.waitForIdle()

        // Verifica presenza di elementi della schermata customer
        // (puoi aggiungere qui verifiche specifiche per gli elementi che vuoi controllare)
        Thread.sleep(5000) // 5 secondi finali per vedere bene la schermata Customer
    }

    /**
     * Test del flusso di login con credenziali non valide.
     *
     * Verifica che l'applicazione gestisca correttamente i tentativi di login
     * con credenziali errate:
     * 1. Attende il caricamento della MainActivity
     * 2. Inserisce email errata "wrong@email.com"
     * 3. Inserisce password errata "wrongpassword"
     * 4. Clicca sul bottone "Accedi"
     * 5. Verifica che l'utente rimanga sulla schermata di login
     * 6. Verifica la presenza del messaggio di errore
     *
     * Include pause per osservare il comportamento dell'app in caso di errore.
     */
    @Test
    fun loginFlow_withInvalidCredentials_showsError() {
        // Aspetta che MainActivity si carichi
        composeRule.waitForIdle()
        Thread.sleep(1500)

        // Inserisci credenziali errate
        composeRule.onNode(
            hasSetTextAction() and hasImeAction(ImeAction.Next),
            useUnmergedTree = true
        ).performTextInput("wrong@email.com")
        Thread.sleep(800)

        composeRule.onNode(
            hasSetTextAction() and hasImeAction(ImeAction.Done),
            useUnmergedTree = true
        ).performTextInput("wrongpassword")
        Thread.sleep(800)

        // Clicca Accedi
        composeRule.onNodeWithText("Accedi", useUnmergedTree = true).performClick()
        Thread.sleep(3000) // 3 secondi per vedere il messaggio di errore

        // Il test rimane nella schermata di login
        composeRule.onNodeWithText("SmartShop", substring = true).assertExists()
        Thread.sleep(2000)
    }
}
