package it.unito.smartshopmobile.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import it.unito.smartshopmobile.data.entity.MapPolygon

/**
 * SupermarketPolygons.kt
 *
 * Adatta il poligono dell'asset JSON alla UI (Offset) e gestisce hit detection.
 */

// Modello per poligoni scaffali
data class ShelfPolygon(
    val id: String,
    val points: List<Offset>,
    val fillColor: Color = Color(0x6632CD32), // verde trasparente
    val strokeColor: Color = Color(0xFF2E8B57),
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

fun MapPolygon.toShelfPolygon(): ShelfPolygon =
    ShelfPolygon(
        id = id.toString(),
        points = points.map { Offset(it.x, it.y) }
    )
