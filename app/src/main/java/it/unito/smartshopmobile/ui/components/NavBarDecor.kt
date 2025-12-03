/**
 * NavBarDecor.kt
 *
 * MVVM: View Layer - Componente UI decorativo
 *
 * FUNZIONAMENTO:
 * - Divisore decorativo per NavigationBar
 * - Gradiente orizzontale con fade-in/fade-out laterale
 * - Separatore visivo tra contenuto e bottom navigation
 *
 * PATTERN MVVM:
 * - View Component: elemento UI riutilizzabile
 * - Stateless: riceve colore come parametro
 * - Material 3: usa MaterialTheme.colorScheme.primary
 */
package it.unito.smartshopmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NavBarDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0f),
                        color.copy(alpha = 0.15f),
                        color.copy(alpha = 0.65f),
                        color.copy(alpha = 0.15f),
                        color.copy(alpha = 0f)
                    )
                )
            )
    )
}
