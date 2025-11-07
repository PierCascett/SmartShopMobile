package it.unito.smartshopmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.unito.smartshopmobile.R
import it.unito.smartshopmobile.data.model.Product
import it.unito.smartshopmobile.data.model.ProductAvailability
import it.unito.smartshopmobile.data.model.ProductCategory
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.AvailabilityFilter
import it.unito.smartshopmobile.viewModel.CatalogUiState

/**
 * Schermata MVVM del catalogo cliente.
 * Nota per quando aggiungeremo retrofit e room: quando la sincronizzazione Room/Retrofit sara' disponibile
 * bastera' osservare lo StateFlow del ViewModel (gia' pronto) senza modificare questa UI.
 */
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    onCategorySelected: (ProductCategory?) -> Unit = {},
    onToggleOffers: () -> Unit = {},
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit = {},
    onBookmark: (String) -> Unit = {},
    onAddToCart: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        CatalogHeader(state.searchQuery, onSearchQueryChange)
        Spacer(modifier = Modifier.height(12.dp))
        CategorySection(
            selected = state.selectedCategory,
            onCategorySelected = onCategorySelected
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilterRow(
            onlyOffers = state.onlyOffers,
            availabilityFilter = state.availabilityFilter,
            onToggleOffers = onToggleOffers,
            onAvailabilityFilterChange = onAvailabilityFilterChange
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.errorMessage != null -> ErrorState(state.errorMessage)

            state.visibleProducts.isEmpty() -> EmptyState(state.searchQuery, state.selectedCategory != null)

            else -> CatalogList(
                products = state.visibleProducts,
                onBookmark = onBookmark,
                onAddToCart = onAddToCart
            )
        }
    }
}
@Composable
private fun ProductCard(
    product: Product,
    onBookmark: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                AvailabilityBadge(
                    product.availability,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                product.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            PriceRow(product)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Rounded.Favorite else Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = if (product.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onAddToCart, shape = RoundedCornerShape(50)) {
                    Text(if (product.isInCart) "Nel carrello" else "Aggiungi")
                }
            }
        }
    }
}

@Composable
private fun PriceRow(product: Product) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "\u20AC ${String.format("%.2f", product.price)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        product.oldPrice?.let {
            Text(
                text = "\u20AC ${String.format("%.2f", it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
        if (product.tags.isNotEmpty()) {
            Text(
                text = product.tags.first(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun AvailabilityBadge(
    availability: ProductAvailability,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (availability) {
        ProductAvailability.AVAILABLE -> "Disponibile" to MaterialTheme.colorScheme.primary
        ProductAvailability.RUNNING_LOW -> "Quasi finito" to MaterialTheme.colorScheme.tertiary
        ProductAvailability.OUT_OF_STOCK -> "Non disponibile" to MaterialTheme.colorScheme.error
    }
    Badge(
        modifier = modifier,
        containerColor = color,
        contentColor = Color.White
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EmptyState(query: String, filteredByCategory: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nessun prodotto trovato",
            style = MaterialTheme.typography.titleMedium
        )
        val reason = when {
            query.isNotBlank() -> "Prova a modificare la ricerca"
            filteredByCategory -> "La categoria scelta e' temporaneamente vuota"
            else -> "Torna piu' tardi per nuovi articoli"
        }
        Text(
            reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Oops, qualcosa e' andato storto", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogScreenPreview() {
    val sampleState = CatalogUiState(
        products = previewProducts(),
        visibleProducts = previewProducts(),
        isLoading = false
    )
    SmartShopMobileTheme {
        CatalogScreen(state = sampleState)
    }
}

@Suppress("MagicNumber")
private fun previewProducts(): List<Product> = listOf(
    Product(
        id = "prev-1",
        name = "Succo ACE",
        brand = "Freshly",
        category = ProductCategory.DRINKS,
        price = 2.59,
        oldPrice = 2.99,
        availability = ProductAvailability.AVAILABLE,
        tags = listOf("Offerta"),
        rating = 4.5,
        reviewsCount = 120
    ),
    Product(
        id = "prev-2",
        name = "Detersivo Piatti",
        brand = "CleanUp",
        category = ProductCategory.CLEANING,
        price = 1.99,
        availability = ProductAvailability.RUNNING_LOW,
        tags = listOf("Formato max"),
        rating = 4.2,
        reviewsCount = 80
    )
)
@Composable
private fun CatalogList(
    products: List<Product>,
    onBookmark: (String) -> Unit,
    onAddToCart: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onBookmark = { onBookmark(product.id) },
                onAddToCart = { onAddToCart(product.id) }
            )
        }
    }
}

@Composable
private fun FilterRow(
    onlyOffers: Boolean,
    availabilityFilter: AvailabilityFilter,
    onToggleOffers: () -> Unit,
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AssistChip(
                onClick = onToggleOffers,
                label = { Text(if (onlyOffers) "Offerte attive" else "Mostra offerte") },
                leadingIcon = {
                    Icon(Icons.Filled.FilterList, contentDescription = null)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (onlyOffers) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            )
        }
        items(AvailabilityFilter.values()) { filter ->
            SuggestionChip(
                onClick = { onAvailabilityFilterChange(filter) },
                label = { Text(filter.label) },
                icon = {
                    if (filter == availabilityFilter) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (filter == availabilityFilter)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun CategorySection(
    selected: ProductCategory?,
    onCategorySelected: (ProductCategory?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ElevatedFilterChip(
                selected = selected == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Tutto") }
            )
        }
        items(ProductCategory.values()) { category ->
            ElevatedFilterChip(
                selected = selected == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.label) }
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Cosa vuoi comprare oggi?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Cerca per prodotto o brand") },
            singleLine = true
        )
    }
}
