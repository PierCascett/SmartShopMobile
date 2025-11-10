/**
 * StoreMapComposable.kt
 *
 * Mappa del supermercato renderizzata con Jetpack Compose Canvas
 * - 100% nativo Android, no WebView
 * - Performante e reattivo
 * - Integrato con Material 3
 */
package it.unito.smartshopmobile.ui.components

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Modello corsia con bounds per hit detection
data class AisleBounds(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val strokeColor: Color,
    val label: String,
    val description: String,
    val textColor: Color = Color.Black
) {
    fun contains(offset: Offset): Boolean {
        return offset.x >= x && offset.x <= x + width &&
               offset.y >= y && offset.y <= y + height
    }
}

@Composable
fun StoreMapCanvas(
    selectedAisleId: String?,
    onAisleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedAisle by remember { mutableStateOf(selectedAisleId) }
    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var lastTapPosition by remember { mutableStateOf<Offset?>(null) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    var dashPhase by remember { mutableStateOf(0f) }
    
    // Definizione corsie con coordinate e colori
    val aisles = remember {
        listOf(
            // RIGA A - Prodotti Freschi
            AisleBounds("A1", 100f, 150f, 300f, 120f, 
                Color(0xFF81C784), Color(0xFF66BB6A), "A1", "Frutta & Verdura"),
            AisleBounds("A2", 450f, 150f, 300f, 120f, 
                Color(0xFFFFB74D), Color(0xFFFFA726), "A2", "Panetteria"),
            AisleBounds("A3", 800f, 150f, 300f, 120f, 
                Color(0xFFE57373), Color(0xFFEF5350), "A3", "Salumeria", Color.White),
            
            // RIGA B - Dispensa
            AisleBounds("B1", 100f, 310f, 300f, 120f, 
                Color(0xFFFFD54F), Color(0xFFFFCA28), "B1", "Pasta e Riso"),
            AisleBounds("B2", 450f, 310f, 300f, 120f, 
                Color(0xFFA1887F), Color(0xFF8D6E63), "B2", "Conserve", Color.White),
            AisleBounds("B3", 800f, 310f, 300f, 120f, 
                Color(0xFFDCE775), Color(0xFFD4E157), "B3", "Condimenti"),
            
            // RIGA C - Bevande e Snack
            AisleBounds("C1", 100f, 470f, 300f, 120f, 
                Color(0xFF64B5F6), Color(0xFF42A5F5), "C1", "Bevande", Color.White),
            AisleBounds("C2", 450f, 470f, 300f, 120f, 
                Color(0xFFF06292), Color(0xFFEC407A), "C2", "Snack e Dolci", Color.White),
            AisleBounds("C3", 800f, 470f, 300f, 120f, 
                Color(0xFF90CAF9), Color(0xFF64B5F6), "C3", "Surgelati"),
            
            // RIGA D - Casa e Pet
            AisleBounds("D1", 100f, 630f, 300f, 120f, 
                Color(0xFFBA68C8), Color(0xFFAB47BC), "D1", "Detersivi", Color.White),
            AisleBounds("D2", 450f, 630f, 300f, 120f, 
                Color(0xFF4DB6AC), Color(0xFF26A69A), "D2", "Igiene", Color.White),
            AisleBounds("D3", 800f, 630f, 300f, 120f, 
                Color(0xFFFF8A65), Color(0xFFFF7043), "D3", "Pet Care", Color.White)
        )
    }
    
    // Animation side-effects
    LaunchedEffect(selectedAisle) {
        // start dash animation when selection changes
        if (selectedAisle != null) {
            dashPhase = 0f
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            dashPhase += 6f
            ripples.removeAll { it.isFinished() }
            delay(16) // ~60fps
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Combined gesture detection: tap + transform
                detectTransformGestures { centroid, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 2.5f)
                    translation += pan
                }
            }
            .pointerInput(selectedAisle) {
                detectTapGestures(
                    onDoubleTap = {
                        // reset view on double tap
                        scale = 1f
                        translation = Offset.Zero
                    },
                    onTap = { tapOffset ->
                        lastTapPosition = tapOffset
                        // Convert tap into world coords (before scaling/panning)
                        val scaleX = size.width / 1200f
                        val scaleY = size.height / 900f
                        val world = (tapOffset - translation) / scale
                        val scaledOffset = Offset(world.x / scaleX, world.y / scaleY)
                        val tappedAisle = aisles.find { it.contains(scaledOffset) }
                        if (tappedAisle != null) {
                            selectedAisle = tappedAisle.id
                            onAisleClick(tappedAisle.id)
                        }
                        // Add ripple on any tap
                        ripples.add(Ripple(center = tapOffset))
                    }
                )
            }
    ) {
        val scaleX = size.width / 1200f
        val scaleY = size.height / 900f
        
        // Gradient background
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFE3F2FD), Color(0xFFF5F5F5))
            )
        )
        
        // Titolo
        drawText(
            textMeasurer = textMeasurer,
            text = "Mappa Supermercato",
            topLeft = Offset((600f - 120f) * scaleX, 40f * scaleY),
            style = TextStyle(
                fontSize = (28 * scaleY).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        )
        
        // Apply pan & zoom with drawContext transform (manual)
        withTransform({
            translate(translation.x, translation.y)
            scale(scale, scale)
        }) {
            // Ingresso (moves with map)
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(500f * scaleX, 70f * scaleY),
                size = Size(200f * scaleX, 40f * scaleY),
                cornerRadius = CornerRadius(5f * scaleX, 5f * scaleY)
            )
            drawRoundRect(
                color = Color(0xFF2E7D32),
                topLeft = Offset(500f * scaleX, 70f * scaleY),
                size = Size(200f * scaleX, 40f * scaleY),
                cornerRadius = CornerRadius(5f * scaleX, 5f * scaleY),
                style = Stroke(width = 2f * scaleX)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "INGRESSO",
                topLeft = Offset(560f * scaleX, 82f * scaleY),
                style = TextStyle(
                    fontSize = (16 * scaleY).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            // Draw optional path from entrance to selected aisle
            selectedAisle?.let { selId ->
                aisles.find { it.id == selId }?.let { target ->
                    drawPathToAisle(target, scaleX, scaleY, dashPhase)
                }
            }
            // Corsie
            aisles.forEach { aisle ->
                drawAisle(
                    aisle = aisle,
                    isSelected = aisle.id == selectedAisle,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    textMeasurer = textMeasurer
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
        
        // Casse
        drawRoundRect(
            color = Color(0xFF757575),
            topLeft = Offset(400f * scaleX, 800f * scaleY),
            size = Size(400f * scaleX, 60f * scaleY),
            cornerRadius = CornerRadius(5f * scaleX, 5f * scaleY)
        )
        drawRoundRect(
            color = Color(0xFF616161),
            topLeft = Offset(400f * scaleX, 800f * scaleY),
            size = Size(400f * scaleX, 60f * scaleY),
            cornerRadius = CornerRadius(5f * scaleX, 5f * scaleY),
            style = Stroke(width = 2f * scaleX)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "CASSE",
            topLeft = Offset(570f * scaleX, 820f * scaleY),
            style = TextStyle(
                fontSize = (18 * scaleY).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

private fun DrawScope.drawAisle(
    aisle: AisleBounds,
    isSelected: Boolean,
    scaleX: Float,
    scaleY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val cornerRadius = CornerRadius(8f * scaleX, 8f * scaleY)
    val topLeft = Offset(aisle.x * scaleX, aisle.y * scaleY)
    val size = Size(aisle.width * scaleX, aisle.height * scaleY)
    
    // Base shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.10f),
        topLeft = topLeft + Offset(6f * scaleX, 6f * scaleY),
        size = size,
        cornerRadius = cornerRadius
    )
    // Rettangolo principale con gradient
    val fillBrush = if (isSelected) Brush.verticalGradient(
        listOf(aisle.color.lighten(0.12f), aisle.color)
    ) else Brush.verticalGradient(listOf(aisle.color, aisle.color.darken(0.08f)))
    drawRoundRect(
        brush = fillBrush,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius
    )
    
    // Bordo pulsante se selezionato
    val borderColor = if (isSelected) Color(0xFF1976D2) else aisle.strokeColor
    val borderWidth = if (isSelected) 5f * scaleX else 2f * scaleX
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = borderWidth)
    )
    
    // Testo ID corsia
    drawText(
        textMeasurer = textMeasurer,
        text = aisle.label,
        topLeft = Offset(
            (aisle.x + aisle.width / 2 - 15f) * scaleX,
            (aisle.y + 30f) * scaleY
        ),
        style = TextStyle(
            fontSize = (16 * scaleY).sp,
            fontWeight = FontWeight.Bold,
            color = aisle.textColor
        )
    )
    
    // Descrizione
    drawText(
        textMeasurer = textMeasurer,
        text = aisle.description,
        topLeft = Offset(
            (aisle.x + aisle.width / 2 - aisle.description.length * 3f) * scaleX,
            (aisle.y + 55f) * scaleY
        ),
        style = TextStyle(
            fontSize = (12 * scaleY).sp,
            color = aisle.textColor.copy(alpha = 0.85f)
        )
    )
}

// Animated path from entrance (center of ingresso rect) to target aisle center
private fun DrawScope.drawPathToAisle(
    target: AisleBounds,
    scaleX: Float,
    scaleY: Float,
    dashPhase: Float
) {
    val ingressCenter = Offset((500f + 100f) * scaleX, (70f + 20f) * scaleY)
    val aisleCenter = Offset((target.x + target.width / 2f) * scaleX, (target.y + target.height / 2f) * scaleY)
    val path = Path().apply {
        moveTo(ingressCenter.x, ingressCenter.y)
        // Simple orthogonal path: vertical then horizontal
        lineTo(ingressCenter.x, aisleCenter.y)
        lineTo(aisleCenter.x, aisleCenter.y)
    }
    val dashIntervals = floatArrayOf(18f, 14f)
    drawPath(
        path = path,
        color = Color(0xFF1976D2),
        style = Stroke(width = 8f * scaleX, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(dashIntervals, dashPhase))
    )
    // Glow overlay
    drawPath(
        path = path,
        color = Color(0xFF1976D2).copy(alpha = 0.25f),
        style = Stroke(width = 16f * scaleX, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(dashIntervals, dashPhase + 10f))
    )
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
