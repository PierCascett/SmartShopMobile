/**
 * ExampleInstrumentedTest.kt
 *
 * Test di strumentazione di esempio eseguito su dispositivo/emulatore.
 * Mantenuto per coerenza documentale con il resto della suite androidTest.
 */
package it.unito.smartshopmobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test di strumentazione di esempio che verifica il contesto dell'applicazione.
 * Questo test viene eseguito su un dispositivo Android reale o emulatore.
 *
 * @see [testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Verifica che il contesto dell'app sia corretto.
     * Il test controlla che il package name dell'applicazione corrisponda
     * a "it.unito.smartshopmobile".
     */
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("it.unito.smartshopmobile", appContext.packageName)
    }
}
