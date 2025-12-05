/**
 * MainDispatcherRule.kt
 *
 * Regola JUnit condivisa per sostituire Dispatchers.Main con un dispatcher di test.
 * Inserita per uniformare la documentazione dei file del modulo test.
 */
package it.unito.smartshopmobile.unitViewModelTest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Test Rule per configurare un dispatcher di test su Dispatchers.Main.
 *
 * Questa regola è necessaria per testare ViewModel che utilizzano viewModelScope,
 * poiché sostituisce il Main dispatcher di Android con un TestDispatcher che funziona
 * in ambiente JVM senza richiedere un dispositivo Android.
 *
 * La regola imposta il dispatcher all'inizio del test e lo ripristina alla fine,
 * garantendo l'isolamento tra i test.
 *
 * @property dispatcher Il dispatcher di test da utilizzare (default: StandardTestDispatcher)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    /**
     * Imposta il dispatcher di test come Main dispatcher prima dell'esecuzione del test.
     *
     * @param description Descrizione del test che sta per essere eseguito
     */
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    /**
     * Ripristina il Main dispatcher originale dopo l'esecuzione del test.
     *
     * @param description Descrizione del test appena completato
     */
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
