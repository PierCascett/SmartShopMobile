/**
 * MapJsonLoader.kt
 *
 * MVVM: View Layer - Loader poligoni mappa da JSON
 *
 * FUNZIONAMENTO:
 * - Carica poligoni scaffali da assets/map/supermarket.json
 * - Downsampling automatico per bitmap grandi (evita crash Canvas)
 * - Parsing JSON: array di array di punti [x, y]
 * - Mapping poligono n → id_scaffale n (1-based)
 * - Ottimizzazione: maxSide e maxPixels per ridimensionamento
 *
 * PATTERN MVVM:
 * - Utility: helper caricamento dati mappa
 * - remember(): caching poligoni
 * - JSON parsing: org.json.JSONArray
 * - Downsampling: BitmapFactory.Options.inSampleSize
 *
 * NOTE:
 * - maxSide: lato massimo bitmap (default 4096px)
 * - maxPixels: budget totale pixel (~16MP)
 */
package it.unito.smartshopmobile.ui.map

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import it.unito.smartshopmobile.data.entity.MapPolygon
import org.json.JSONArray
import org.json.JSONException

// Loader per bitmap mappa e poligoni scaffali da assets.
// Downsampling automatico per evitare crash di Canvas con bitmap enormi.
// - image: assets/map/*.png
// - polygons: assets/map/supermarket.json (array di array di punti x,y)
// Poligono n -> id_scaffale n (1-based, coerente col dump scaffali).

/**
 * Carica un'immagine dagli assets riducendone le dimensioni se necessario.
 *
 * @param path Path relativo nella cartella assets
 * @param maxSide Lato massimo consentito dopo il downsampling
 * @param maxPixels Budget massimo di pixel totali per evitare OOM
 * @return `ImageBitmap` pronto per Compose o null in caso di errore
 */
@Composable
fun rememberAssetImage(
    path: String,
    maxSide: Int = 4096,          // lato massimo desiderato (larghezza o altezza)
    maxPixels: Int = 16_000_000    // budget massimo (larghezza * altezza) circa ~16MP
): ImageBitmap? {
    val context = LocalContext.current
    return remember(path to maxSide to maxPixels) {
        try {
            // 1. Prima apertura solo per leggere dimensioni (inJustDecodeBounds)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            }
            val outW = boundsOptions.outWidth
            val outH = boundsOptions.outHeight
            if (outW <= 0 || outH <= 0) {
                Log.e("MapImageLoader", "Dimensioni non valide per $path")
                return@remember null
            }

            // 2. Calcola inSampleSize (potenze di 2) in base ai limiti
            val sampleSize = calculateInSampleSize(outW, outH, maxSide, maxPixels)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // dimezza memoria rispetto a ARGB_8888 se alfa non necessario
            }

            // 3. Seconda apertura per decodifica reale downsampled
            val bitmap = context.assets.open(path).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            }
            if (bitmap == null) {
                Log.e("MapImageLoader", "Decodifica bitmap fallita per $path")
                return@remember null
            }
            Log.i(
                "MapImageLoader",
                "Caricata $path (${outW}x${outH}) -> downsample inSampleSize=$sampleSize => ${bitmap.width}x${bitmap.height} (~${bitmap.byteCount} bytes)"
            )
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            Log.e("MapImageLoader", "Errore caricamento immagine $path", e)
            null
        }
    }
}
/**
 * Calcola il fattore di downsampling (potenza di 2) in base ai limiti di lato e pixel.
 *
 * @return inSampleSize da usare nelle BitmapFactory.Options
 */
private fun calculateInSampleSize(outW: Int, outH: Int, maxSide: Int, maxPixels: Int): Int {
    var sample = 1
    // Riduci finché eccede i limiti
    while (true) {
        val targetW = outW / sample
        val targetH = outH / sample
        val tooBigSide = targetW > maxSide || targetH > maxSide
        val tooManyPixels = (targetW.toLong() * targetH.toLong()) > maxPixels.toLong()
        if (tooBigSide || tooManyPixels) {
            sample *= 2
        } else {
            break
        }
    }
    return sample.coerceAtLeast(1)
}

/**
 * Carica e memoizza i poligoni della mappa da un file JSON negli assets.
 *
 * @param path Path relativo del JSON (es. "map/supermarket.json")
 * @return Lista di `MapPolygon` o lista vuota in caso di errore
 */
@Composable
fun rememberPolygonsFromJson(path: String): List<MapPolygon> {
    val context = LocalContext.current
    return remember(path) {
        try {
            val jsonText = context.assets.open(path).bufferedReader().use { it.readText() }
            parsePolygons(jsonText)
        } catch (e: Exception) {
            Log.e("MapJsonLoader", "Errore caricamento poligoni $path", e)
            emptyList()
        }
    }
}
/**
 * Effettua il parsing del JSON dei poligoni e lo mappa in `MapPolygon`.
 *
 * @param jsonText Contenuto del file JSON con array di poligoni
 * @return Lista di poligoni con coordinate convertite in float
 */
private fun parsePolygons(jsonText: String): List<MapPolygon> {
    val result = mutableListOf<MapPolygon>()
    try {
        val arr = JSONArray(jsonText)
        for (i in 0 until arr.length()) {
            val poly = arr.getJSONArray(i)
            val points = mutableListOf<MapPolygon.Point>()
            for (j in 0 until poly.length()) {
                val point = poly.getJSONObject(j)
                val x = point.getDouble("x").toFloat()
                val y = point.getDouble("y").toFloat()
                points.add(MapPolygon.Point(x, y))
            }
            result.add(
                MapPolygon(
                    id = i + 1, // poligono n -> scaffale id n
                    points = points
                )
            )
        }
    } catch (e: JSONException) {
        Log.e("MapJsonLoader", "Errore parse poligoni", e)
    }
    return result
}
