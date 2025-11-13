package it.unito.smartshopmobile.ui.map

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * MapImageLoader.kt
 * 
 * Utility per caricare immagini dalla cartella assets dell'app.
 * 
 * USAGE:
 * ```kotlin
 * val mapImage = rememberAssetImage("map/supermarket.jpg")
 * StoreMapCanvas(background = mapImage, ...)
 * ```
 * 
 * FILE LOCATION:
 * - Metti l'immagine in: app/src/main/assets/map/supermarket.jpg
 * - Il path relativo "map/supermarket.jpg" verrà risolto dal context.assets
 * 
 * NOTE:
 * - Se il file non esiste o c'è un errore di decodifica, ritorna null
 * - L'immagine viene caricata in memoria: ottimizza dimensioni se troppo grande
 * - Il composable remember() cachea il risultato per evitare reload
 */

/**
 * Load an ImageBitmap from the app assets folder.
 * Place your file under app/src/main/assets and pass the relative path, e.g. "map/supermarket.jpg".
 */
@Composable
fun rememberAssetImage(assetPath: String, maxDimension: Int = 2048): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { input ->
                // Decode con downsampling per evitare OutOfMemory
                val options = BitmapFactory.Options().apply {
                    // Prima pass: leggi solo dimensioni
                    inJustDecodeBounds = true
                }
                context.assets.open(assetPath).use { measureInput ->
                    BitmapFactory.decodeStream(measureInput, null, options)
                }
                
                // Calcola sample size per ridurre memoria
                val (width, height) = options.outWidth to options.outHeight
                var sampleSize = 1
                while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
                    sampleSize *= 2
                }
                
                // Seconda pass: decodifica con riduzione
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                context.assets.open(assetPath).use { decodeInput ->
                    BitmapFactory.decodeStream(decodeInput, null, decodeOptions)?.asImageBitmap()
                }
            }
        }.getOrNull()
    }
}
