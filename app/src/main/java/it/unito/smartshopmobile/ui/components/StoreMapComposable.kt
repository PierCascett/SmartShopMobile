/**
 * StoreMapComposable.kt
 *
 * Mappa del supermercato renderizzata con Jetpack Compose Canvas
 * - 100% nativo Android, no WebView
 * - Performante e reattivo
 * - Integrato con Material 3
 * 
 * ARCHITETTURA:
 * - Background: Immagine planimetria (supermarket.jpg) caricata da assets/map/
 * - Overlay: Poligoni scaffali definiti in SupermarketPolygons.kt
 * - Interazioni: Pan, zoom (pinch), tap per selezione scaffale
 * 
 * COORDINATE SYSTEM:
 * - I poligoni sono definiti in pixel dell'immagine originale
 * - La canvas scala automaticamente immagine e poligoni insieme
 * - baseWidth/baseHeight: dimensioni immagine (o fallback 7000x5000)
 * - scaleX/scaleY: fattori di scala per adattare a dimensioni canvas
 * 
 * RENDERING PIPELINE:
 * 1. Disegna sfondo (immagine o gradient)
 * 2. Applica trasformazioni (pan + zoom)
 * 3. Disegna percorso animato dall'ingresso allo scaffale selezionato
 * 4. Disegna ogni poligono con ombra, riempimento, bordo e label
 * 5. Disegna effetti ripple su tap (fuori dalla trasformazione)
 */
package it.unito.smartshopmobile.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import it.unito.smartshopmobile.ui.map.SupermarketPolygons
import it.unito.smartshopmobile.ui.map.ShelfPolygon
import it.unito.smartshopmobile.ui.map.rememberPolygonsFromJson

// Helper: point-in-polygon usando vertici trasformati nello spazio schermo
private fun isPointInPolygonScreen(
    tap: Offset,
    polygon: ShelfPolygon,
    scale: Float,
    translation: Offset,
    imgOffsetX: Float,
    imgOffsetY: Float,
    scaleX: Float,
    scaleY: Float
): Boolean {
    // Trasforma ogni vertice nella posizione sullo schermo
    val screenPoints = polygon.points.map { p ->
        val sx = p.x * scaleX * scale + translation.x + imgOffsetX
        val sy = p.y * scaleY * scale + translation.y + imgOffsetY
        Offset(sx, sy)
    }
    // Ray-casting sullo spazio schermo
    var result = false
    var j = screenPoints.size - 1
    for (i in screenPoints.indices) {
        val pi = screenPoints[i]
        val pj = screenPoints[j]
        val intersect = ((pi.y > tap.y) != (pj.y > tap.y)) &&
            (tap.x < (pj.x - pi.x) * (tap.y - pi.y) / (pj.y - pi.y + 0.00001f) + pi.x)
        if (intersect) result = !result
        j = i
    }
    return result
}

// Modello corsia con bounds per hit detection
// Rettangoli placeholder rimossi: ora usiamo poligoni personalizzati

@Composable
fun StoreMapCanvas(
    selectedAisleId: String?,
    onAisleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    background: ImageBitmap? = null // opzionale: immagine mappa (supermarket.jpg)
) {
    // Usare le dimensioni originali dell'immagine di riferimento per i poligoni
    // (le coordinate in SupermarketPolygons sono espresse per 11278x8596)
    val ORIGINAL_IMAGE_WIDTH = 11278f
    val ORIGINAL_IMAGE_HEIGHT = 8596f

    // Non manteniamo uno stato locale separato per la selezione: usiamo direttamente
    // il parametro `selectedAisleId` così che la selezione nel ViewModel ricomponi la mappa.
    val selectedAisle = selectedAisleId
    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val jsonPolygons = rememberPolygonsFromJson("map/supermarket.json")
    val polygons = if (jsonPolygons.isNotEmpty()) jsonPolygons else SupermarketPolygons.polygons
    // Stato condiviso tra draw e pointerInput per consentire al pointerInput
    // di trasformare immediatamente i tap in coordinate immagine.
    var currentImgWidth by remember { mutableStateOf(0f) }
    var currentImgHeight by remember { mutableStateOf(0f) }
    var currentImgOffsetX by remember { mutableStateOf(0f) }
    var currentImgOffsetY by remember { mutableStateOf(0f) }
    // Usa la dimensione originale dell'immagine come riferimento (coerente con SupermarketPolygons).
    // Se in futuro i poligoni fossero definiti per un'altra immagine, aggiorna questi valori.
    val baseWidth = ORIGINAL_IMAGE_WIDTH
    val baseHeight = ORIGINAL_IMAGE_HEIGHT

    // Calcola aspect ratio e offset una volta
    val imgAspect = baseWidth / baseHeight
    
    // Animation side-effects (solo ripple)
    // Avvia la coroutine solo quando ci sono ripples e riavviala quando cambia il numero di ripples.
    // Questo evita di avere una coroutine in loop continuo che causa ricomposizioni costanti.
    LaunchedEffect(ripples.size) {
        while (ripples.isNotEmpty()) {
            ripples.removeAll { it.isFinished() }
            delay(16) // ~60fps
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Combined gesture detection: transform gestures handled first
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 2.5f)
                    translation += pan
                }
            }
            .pointerInput(Unit) {
                // Tap detection after transform to reduce conflicts
                detectTapGestures(
                    onDoubleTap = {
                        // reset view on double tap
                        scale = 1f
                        translation = Offset.Zero
                    },
                    onTap = { tapOffset ->
                        // Add ripple on any tap (visual feedback)
                        ripples.add(Ripple(center = tapOffset))

                        // Try to resolve hit immediately using the latest image geometry
                        val imgW = currentImgWidth
                        val imgH = currentImgHeight
                        val imgOX = currentImgOffsetX
                        val imgOY = currentImgOffsetY
                        if (imgW > 0f && imgH > 0f) {
                             val scaleXLocal = imgW / baseWidth
                             val scaleYLocal = imgH / baseHeight

                            // Convert screen tap to IMAGE coordinates (inverse transform)
                            val imgPoint = Offset(
                                x = (tapOffset.x - imgOX - translation.x) / (scale * scaleXLocal),
                                y = (tapOffset.y - imgOY - translation.y) / (scale * scaleYLocal)
                            )

                            // Precise hit test in IMAGE space using polygon.containsPoint
                            var tapped = polygons.asReversed().firstOrNull { p ->
                                p.containsPoint(imgPoint)
                            }

                            // Screen-space fallback using transformed vertices (more robust for slight mismatches)
                            if (tapped == null) {
                                tapped = polygons.asReversed().firstOrNull { p ->
                                    isPointInPolygonScreen(
                                        tap = tapOffset,
                                        polygon = p,
                                        scale = scale,
                                        translation = translation,
                                        imgOffsetX = imgOX,
                                        imgOffsetY = imgOY,
                                        scaleX = scaleXLocal,
                                        scaleY = scaleYLocal
                                    )
                                }
                            }

                            // Fallback: nearest centroid in screen space (robusto se polygons slightly mismatch)
                            if (tapped == null) {
                                val thresholdPx = 60f // screen pixels
                                var best: ShelfPolygon? = null
                                var bestDist = Float.MAX_VALUE
                                polygons.forEach { p ->
                                    val c = Offset(
                                        p.centroid().x * scaleXLocal * scale + translation.x + imgOX,
                                        p.centroid().y * scaleYLocal * scale + translation.y + imgOY
                                    )
                                    val dx = c.x - tapOffset.x
                                    val dy = c.y - tapOffset.y
                                    val d2 = dx * dx + dy * dy
                                    if (d2 < bestDist) {
                                        bestDist = d2
                                        best = p
                                    }
                                }
                                if (best != null && bestDist <= thresholdPx * thresholdPx) tapped = best
                            }

                            Log.d("StoreMap", "pointer tap raw=$tapOffset img=$imgPoint tapped=${tapped?.id}")
                            tapped?.let { onAisleClick(it.id) }
                        } else {
                            // If geometry not yet available, do nothing (unlikely)
                        }
                     }
                )
            }
    ) {
        // Calcola dimensioni e posizione effettiva dell'immagine (con aspect ratio)
        val canvasAspect = size.width / size.height

        val imgWidth: Float
        val imgHeight: Float
        val imgOffsetX: Float
        val imgOffsetY: Float
        if (canvasAspect > imgAspect) {
            // Canvas più larga: adatta altezza
            imgHeight = size.height
            imgWidth = imgHeight * imgAspect
            imgOffsetX = (size.width - imgWidth) / 2f
            imgOffsetY = 0f
        } else {
            // Canvas più alta: adatta larghezza
            imgWidth = size.width
            imgHeight = imgWidth / imgAspect
            imgOffsetX = 0f
            imgOffsetY = (size.height - imgHeight) / 2f
        }

        // Scale factors basati sull'immagine effettivamente mostrata
        val scaleX = imgWidth / baseWidth
        val scaleY = imgHeight / baseHeight

        // Salva l'ultima geometria dell'immagine per l'uso dal pointerInput
        currentImgWidth = imgWidth
        currentImgHeight = imgHeight
        currentImgOffsetX = imgOffsetX
        currentImgOffsetY = imgOffsetY

        // Sfondo: immagine mappa se disponibile, altrimenti gradient
        if (background != null) {
            drawImage(
                image = background,
                dstOffset = IntOffset(imgOffsetX.toInt(), imgOffsetY.toInt()),
                dstSize = IntSize(imgWidth.toInt(), imgHeight.toInt())
            )
        } else {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFE3F2FD), Color(0xFFF5F5F5))
                )
            )
        }
        
        // Apply pan & zoom with drawContext transform (manual)
        // Important: apply SCALE first then TRANSLATE so translation is not scaled.
        // This ensures the forward mapping is: screen = imgOffset + translation + scale * content
        withTransform({
            scale(scale, scale)
            translate(translation.x + imgOffsetX, translation.y + imgOffsetY)
        }) {
            // Scaffali (poligoni) - disegnare anche il numero di zona
            polygons.forEachIndexed { index, poly ->
                drawPolygonShelf(
                    polygon = poly,
                    isSelected = poly.id == selectedAisle,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    number = index + 1
                )
            }
        }

        // Draw ripples (screen space so they don't scale with map)
        ripples.forEach { ripple ->
            val progress = ripple.progress()
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val radius = 20f + 140f * progress
            drawCircle(
                color = Color(0xFF1976D2).copy(alpha = 0.22f * alpha),
                radius = radius,
                center = ripple.center,
                style = Stroke(width = 4f * (1f - progress))
            )
        }
        
        // Elementi extra (es. casse) rimossi finché non mappati sull'immagine
    }
}

private fun DrawScope.drawPolygonShelf(
    polygon: ShelfPolygon,
    isSelected: Boolean,
    scaleX: Float,
    scaleY: Float,
    number: Int
) {
    // Ombra semplice
    val shadowPath = Path().apply {
        polygon.points.forEachIndexed { index, p ->
            val pt = Offset(p.x * scaleX + 6f * scaleX, p.y * scaleY + 6f * scaleY)
            if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
        }
        close()
    }
    drawPath(
        path = shadowPath,
        color = Color.Black.copy(alpha = 0.10f)
    )
    // Riempimento
    val path = Path().apply {
        polygon.points.forEachIndexed { index, p ->
            val pt = Offset(p.x * scaleX, p.y * scaleY)
            if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
        }
        close()
    }
    val fillBrush = if (isSelected) Brush.verticalGradient(
        listOf(polygon.fillColor.lighten(0.12f), polygon.fillColor)
    ) else Brush.verticalGradient(listOf(polygon.fillColor, polygon.fillColor.darken(0.08f)))
    drawPath(path = path, brush = fillBrush)
    // Bordo
    val borderColor = if (isSelected) Color(0xFF1976D2) else polygon.strokeColor
    val borderWidth = if (isSelected) 5f * scaleX else 2f * scaleX
    drawPath(path = path, color = borderColor, style = Stroke(width = borderWidth))
    // Draw numeric badge at centroid (small circle with number)
    val c = polygon.centroid()
    val cx = c.x * scaleX
    val cy = c.y * scaleY
    val badgeRadius = 20f * ((scaleX + scaleY) / 2f)
    drawCircle(
        color = if (isSelected) Color(0xFF1976D2) else Color.White,
        radius = badgeRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.12f),
        radius = badgeRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 2f * ((scaleX + scaleY) / 2f))
    )
    // Number text: use native canvas via drawContext.canvas.nativeCanvas to avoid extension issues
    val textSizePx = (14f * ((scaleX + scaleY) / 2f)).coerceIn(10f, 24f)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = textSizePx
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    // vertically center text using paint metrics
    val baseline = cy - (paint.descent() + paint.ascent()) / 2f
    drawContext.canvas.nativeCanvas.drawText(number.toString(), cx, baseline, paint)
}

// Ripple model
private data class Ripple(val center: Offset, val startTime: Long = System.currentTimeMillis()) {
    fun progress(): Float = ((System.currentTimeMillis() - startTime).toFloat() / 650f).coerceIn(0f, 1f)
    fun isFinished(): Boolean = progress() >= 1f
}

// Utility color lighten/darken extensions
private fun Color.lighten(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = (red + (1f - red) * a).coerceIn(0f, 1f),
        green = (green + (1f - green) * a).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * a).coerceIn(0f, 1f),
        alpha = alpha
    )
}
private fun Color.darken(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = (red * (1f - a)).coerceIn(0f, 1f),
        green = (green * (1f - a)).coerceIn(0f, 1f),
        blue = (blue * (1f - a)).coerceIn(0f, 1f),
        alpha = alpha
    )
}
