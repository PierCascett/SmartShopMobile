/**
 * MapPolygon.kt
 *
 * MVVM: Model Layer - Entity poligono mappa
 *
 * FUNZIONAMENTO:
 * - Rappresenta poligono scaffale caricato da JSON assets
 * - File: assets/map/supermarket.json
 * - Points: lista coordinate (x, y) in pixel immagine
 * - id: numero scaffale (mapping 1-based con database)
 *
 * PATTERN MVVM:
 * - Entity: dati grezzi da asset JSON
 * - Data class immutabile
 * - Nested Point: coordinate (x, y)
 * - Convertito a ShelfPolygon per UI
 */
package it.unito.smartshopmobile.data.entity

/**
 * Poligono mappa caricato da assets (supermarket.json).
 */
data class MapPolygon(
    val id: Int,
    val points: List<Point>
) {
    data class Point(val x: Float, val y: Float)
}


