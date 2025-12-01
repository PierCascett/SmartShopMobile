/**
 * ProductImage.kt
 *
 * MVVM: View Layer - Componente immagine prodotto
 *
 * FUNZIONAMENTO:
 * - Carica immagine prodotto da server backend
 * - URL: http://<host>:<port>/images/products/<productId>.png
 * - Coil AsyncImage per loading asincrono
 * - Placeholder/Loading/Error states gestiti
 * - Cache automatica con Coil
 *
 * PATTERN MVVM:
 * - View Component: elemento UI riutilizzabile
 * - Coil: libreria image loading Android
 * - SubcomposeAsyncImage: gestione stati loading
 * - ContentScale: adatta immagine a dimensioni
 */
package it.unito.smartshopmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import it.unito.smartshopmobile.data.remote.RetrofitInstance

/**
 * Componente per visualizzare l'immagine di un prodotto dal server backend.
 *
 * Carica l'immagine dal server usando l'ID del prodotto.
 * URL formato: http://<host>:<port>/images/products/<productId>.png
 *
 * @param productId ID del prodotto (es. "prd-01")
 * @param modifier Modificatore per personalizzare il componente
 * @param size Dimensione dell'immagine (default 96.dp)
 * @param contentScale Scala del contenuto (default ContentScale.Fit)
 * @param showPlaceholder Mostra uno sfondo grigio durante il caricamento
 * @param contentDescription Descrizione per accessibilità
 */
@Composable
fun ProductImage(
    productId: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentScale: ContentScale = ContentScale.Fit,
    showPlaceholder: Boolean = true,
    contentDescription: String? = "Immagine prodotto"
) {
    // Ottieni l'host e la porta dal BuildConfig o da una configurazione
    // Modifica questi valori in base al tuo ambiente
    val baseUrl = RetrofitInstance.assetBaseUrl.removeSuffix("/")
    val imageUrl = "$baseUrl/images/products/$productId.png"

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = contentScale
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                if (showPlaceholder) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            is AsyncImagePainter.State.Error -> {
                // Mostra un placeholder in caso di errore
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // Potresti mostrare un'icona di errore o un'immagine placeholder
                }
            }
            else -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}

/**
 * Variante semplificata senza placeholder
 */
@Composable
fun ProductImageSimple(
    productId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Immagine prodotto"
) {
    val baseUrl = RetrofitInstance.assetBaseUrl.removeSuffix("/")
    val imageUrl = "$baseUrl/images/products/$productId.png"

    coil.compose.AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit
    )
}

/**
 * Esempio di utilizzo in una Card di prodotto
 */
@Composable
fun ProductCardWithImage(
    productId: String,
    productName: String,
    productPrice: String
) {
    androidx.compose.material3.Card(
        modifier = Modifier.size(width = 160.dp, height = 200.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Immagine del prodotto
            ProductImage(
                productId = productId,
                size = 120.dp,
                contentDescription = "Immagine di $productName"
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

            // Nome prodotto
            androidx.compose.material3.Text(
                text = productName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            // Prezzo
            androidx.compose.material3.Text(
                text = productPrice,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

