/**
 * DataStoreTestHelper.kt
 *
 * Helper per isolare DataStore nei test di integrazione.
 *
 * Problema: La delegazione statica `by preferencesDataStore(name)` mantiene una cache
 * a livello di Context che persiste tra test, causando conflitti quando più test
 * creano istanze dello stesso DataStore in parallelo.
 *
 * Soluzione: Utilizzare un TestScope condiviso per ogni test e file completamente
 * isolati per evitare conflitti tra test paralleli.
 */
package it.unito.smartshopmobile.testUtils

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File
import java.util.UUID

/**
 * Factory per creare contesti isolati per test con DataStore.
 *
 * Ogni contesto ha cartelle dedicate per files e cache, evitando conflitti
 * quando test paralleli accedono allo stesso DataStore.
 *
 * Uso:
 * ```kotlin
 * private lateinit var testScope: TestScope
 *
 * @Before
 * fun setup() {
 *     testScope = TestScope(UnconfinedTestDispatcher())
 *     val (context, cleanup) = DataStoreTestHelper.createIsolatedContext()
 *     testContext = context
 *     testCleanup = cleanup
 *     testDataStore = DataStoreTestHelper.createDataStore(testContext, "my_datastore", testScope)
 * }
 *
 * @After
 * fun tearDown() {
 *     testScope.cancel()
 *     testCleanup()
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
object DataStoreTestHelper {

    /**
     * Crea un contesto isolato con cartelle dedicate per DataStore.
     *
     * @return Pair di (Context isolato, funzione di cleanup)
     */
    fun createIsolatedContext(): Pair<Context, () -> Unit> {
        val baseContext: Context = ApplicationProvider.getApplicationContext()
        val prefix = "ds_test_${System.nanoTime()}_${UUID.randomUUID()}"
        val root = File(baseContext.cacheDir, prefix)
        val filesDir = File(root, "files").apply { mkdirs() }
        val cacheDir = File(root, "cache").apply { mkdirs() }

        val isolatedContext = object : ContextWrapper(baseContext) {
            override fun getFilesDir(): File = filesDir
            override fun getCacheDir(): File = cacheDir
            override fun getApplicationContext(): Context = this
        }

        val cleanup: () -> Unit = {
            // Elimina la directory
            try {
                root.deleteRecursively()
            } catch (_: Exception) {
                // Ignora errori di cleanup
            }
        }

        return Pair(isolatedContext, cleanup)
    }

    /**
     * Crea un DataStore per i test con nome univoco per evitare collisioni.
     *
     * IMPORTANTE: Lo scope passato deve essere lo stesso usato nel test (TestScope)
     * per evitare conflitti di concorrenza. Il chiamante è responsabile della
     * cancellazione dello scope nel tearDown.
     *
     * @param context Contesto isolato (da createIsolatedContext)
     * @param dataStoreName Nome base per il DataStore (es: "favorites_datastore")
     * @param scope CoroutineScope da usare per il DataStore (preferibilmente TestScope)
     * @return DataStore<Preferences> univoco per questo test
     */
    fun createDataStore(
        context: Context,
        dataStoreName: String,
        scope: CoroutineScope? = null
    ): DataStore<Preferences> {
        val uniqueName = "${dataStoreName}_${System.nanoTime()}_${UUID.randomUUID()}"

        // Usa lo scope fornito o crea uno nuovo con UnconfinedTestDispatcher
        val actualScope = scope ?: CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

        // Crea il file direttamente nella filesDir del context isolato
        val datastoreFile = File(context.filesDir, "datastore/$uniqueName.preferences_pb")
        datastoreFile.parentFile?.mkdirs()

        return PreferenceDataStoreFactory.create(
            scope = actualScope
        ) {
            datastoreFile
        }
    }
}

