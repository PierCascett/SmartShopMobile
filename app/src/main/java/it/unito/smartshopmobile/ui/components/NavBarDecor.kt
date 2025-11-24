package it.unito.smartshopmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

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
