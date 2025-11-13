package it.unito.smartshopmobile.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * SupermarketPolygons.kt
 *
 * Definisce i poligoni degli scaffali del supermercato.
 * 
 * COORDINATE: Le coordinate (x, y) dei vertici sono espresse nei pixel dell'immagine
 * originale (supermarket.jpg). Quando l'immagine viene renderizzata, il sistema di
 * coordinate viene scalato automaticamente per adattarsi alla dimensione della canvas,
 * mantenendo la proporzione corretta tra immagine e poligoni.
 * 
 * SCALING: In StoreMapComposable:
 * - scaleX = canvasWidth / imageWidth
 * - scaleY = canvasHeight / imageHeight
 * - Ogni punto (x, y) viene trasformato in (x * scaleX, y * scaleY)
 * 
 * HIT DETECTION: Usa ray-casting algorithm per determinare se un punto tocca il poligono.
 */

// Modello per poligoni scaffali
data class ShelfPolygon(
    val id: String,
    val points: List<Offset>,
    val fillColor: Color = Color(0x66FF0000), // rosso semi-trasparente
    val strokeColor: Color = Color(0xFFFF0000),
    val textColor: Color = Color.Black,
    val label: String? = null
) {
    // Punto nel poligono (ray casting)
    fun containsPoint(p: Offset): Boolean {
        var result = false
        var j = points.size - 1
        for (i in points.indices) {
            val pi = points[i]
            val pj = points[j]
            val intersect = ((pi.y > p.y) != (pj.y > p.y)) &&
                (p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y + 0.00001f) + pi.x)
            if (intersect) result = !result
            j = i
        }
        return result
    }
    // Centroid approssimato (media dei vertici)
    fun centroid(): Offset {
        val n = points.size
        if (n == 0) return Offset.Zero
        var sx = 0f
        var sy = 0f
        points.forEach { sx += it.x; sy += it.y }
        return Offset(sx / n, sy / n)
    }
}

object SupermarketPolygons {
    // Coordinate per immagine 11278x8596px
    val polygons: List<ShelfPolygon> = listOf(
        ShelfPolygon(
            id = "S1",
            points = listOf(
                Offset(1407f, 4150f),
                Offset(2644f, 3474f),
                Offset(2891f, 4595f),
                Offset(1720f, 5254f),
                Offset(1456f, 4166f),
                Offset(1638f, 4133f),
                Offset(1555f, 4100f)
            ),
            label = "Scaffale 1"
        ),
        ShelfPolygon(
            id = "S2",
            points = listOf(
                Offset(2685f, 3408f),
                Offset(3905f, 2748f),
                Offset(4120f, 3836f),
                Offset(2982f, 4545f),
                Offset(2702f, 3408f)
            ),
            label = "Scaffale 2"
        ),
        ShelfPolygon(
            id = "S3",
            points = listOf(
                Offset(3922f, 2699f),
                Offset(5060f, 2039f),
                Offset(5389f, 3160f),
                Offset(4219f, 3836f),
                Offset(3971f, 2732f)
            ),
            label = "Scaffale 3"
        ),
        ShelfPolygon(
            id = "S4",
            points = listOf(
                Offset(5208f, 1973f),
                Offset(6379f, 1281f),
                Offset(6643f, 2435f),
                Offset(5472f, 3127f),
                Offset(5225f, 2039f),
                Offset(5258f, 1973f)
            ),
            label = "Scaffale 4"
        )
    )
}
