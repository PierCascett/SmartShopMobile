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

