package it.unito.smartshopmobile.uiTest.customer

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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

@RunWith(AndroidJUnit4::class)
class CustomerProfileUpdateUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val customerEmail = "casc@gmail.com"
    private val customerPassword = "casc"

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

    private fun isCatalogVisible(): Boolean {
        val offers = composeRule.onAllNodesWithText("Mostra offerte", useUnmergedTree = true)
        val activeOffers = composeRule.onAllNodesWithText("Offerte attive", useUnmergedTree = true)
        return offers.fetchSemanticsNodes().isNotEmpty() || activeOffers.fetchSemanticsNodes().isNotEmpty()
    }

    private fun pause(ms: Long = 1200) {
        Thread.sleep(ms)
    }
}
