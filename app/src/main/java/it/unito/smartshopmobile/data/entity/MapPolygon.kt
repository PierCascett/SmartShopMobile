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
 * Rappresenta un poligono dello scaffale caricato dal file JSON della mappa.
 *
 * I poligoni vengono caricati da assets/map/supermarket.json e definiscono
 * le aree cliccabili sulla mappa del supermercato. Ogni poligono corrisponde
 * a uno scaffale fisico e il suo ID deve matchare con l'ID dello Shelf nel database.
 *
 * Le coordinate dei punti sono in pixel relativi all'immagine della mappa.
 *
 * Utilizzo:
 * - Caricato da MapJsonLoader all'avvio
 * - Convertito in ShelfPolygon per rendering Compose
 * - Usato per hit-detection (touch events sulla mappa)
 *
 * @property id ID univoco dello scaffale (corrisponde a Shelf.id nel database)
 * @property points Lista di coordinate che definiscono i vertici del poligono
 */
data class MapPolygon(
    val id: Int,
    val points: List<Point>
) {
    /**
     * Rappresenta un punto (vertice) nel sistema di coordinate della mappa.
     *
     * Le coordinate sono espresse in pixel relativi all'immagine originale della mappa.
     * Durante il rendering vengono scalate in base alle dimensioni dello schermo.
     *
     * @property x Coordinata X in pixel
     * @property y Coordinata Y in pixel
     */
    data class Point(val x: Float, val y: Float)
}


