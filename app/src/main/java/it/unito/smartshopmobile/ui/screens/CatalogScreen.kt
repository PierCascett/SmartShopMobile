/**
 * CatalogScreen.kt
 *
 * RUOLO MVVM: View Layer (UI - Jetpack Compose)
 * - Schermata principale del catalogo prodotti per il ruolo Customer
 * - Puramente presentazionale: visualizza stato e invia eventi al ViewModel
 * - Reattiva: si ricompone automaticamente quando lo stato cambia
 *
 * RESPONSABILITÀ:
 * - Osservare stato UI dal CatalogViewModel (collectAsState)
 * - Renderizzare UI in base allo stato (loading, error, success)
 * - Inviare intent utente al ViewModel (search, filter, addToCart)
 * - Gestire navigazione (carrello, dettaglio prodotto)
 * - NO logica business: solo composizione UI
 *
 * PATTERN: MVVM - View
 * - @Composable: funzioni dichiarative per UI
 * - State hoisting: stato gestito dal ViewModel
 * - Unidirectional Data Flow: eventi → ViewModel → stato → UI
 * - NON conosce Repository, Room, Retrofit
 *
 * COMPONENTI PRINCIPALI:
 * - CatalogScreen: schermata principale
 * - SearchBar: input ricerca prodotti
 * - FilterRow: filtri categoria/disponibilità
 * - ProductGrid: griglia prodotti
 * - ProductCard: card singolo prodotto
 * - CartOverlay: overlay carrello laterale
 */
package it.unito.smartshopmobile.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.unito.smartshopmobile.R
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.ui.theme.SmartShopMobileTheme
import it.unito.smartshopmobile.viewModel.AvailabilityFilter
import it.unito.smartshopmobile.viewModel.CartItemUi
import it.unito.smartshopmobile.viewModel.CatalogUiState
import it.unito.smartshopmobile.viewModel.DeliveryMethod
import it.unito.smartshopmobile.viewModel.CustomerOrderHistoryEntry
import it.unito.smartshopmobile.data.remote.RetrofitInstance
val Product.tagsList: List<String> get() = tags ?: emptyList()
val Product.isOutOfStock: Boolean get() = catalogQuantity <= 0

private fun Product.assetImageUrl(): String {
    val remote = imageUrl?.takeIf { it.isNotBlank() }
    if (remote != null) {
        if (remote.startsWith("http", ignoreCase = true)) return remote
        RetrofitInstance.buildAssetUrl(remote)?.let { return it }
    }
    return "${RetrofitInstance.assetBaseUrl}images/products/$id.png"
}

/**
 * Schermata MVVM del catalogo cliente con tre colonne principali:
 * - menu categorie del supermercato (a sinistra)
 * - elenco prodotti filtrabile (al centro)
 * - pannello carrello sintetico (a destra)
 */
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onToggleOffers: () -> Unit = {},
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit = {},
    onTagToggle: (String) -> Unit = {},
    onBookmark: (String) -> Unit = {},
    onAddToCart: (String) -> Unit = {},
    onDecreaseCartItem: (String) -> Unit = {},
    onRemoveFromCart: (String) -> Unit = {},
    onProductClick: (String?) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            val headerTitle = when {
                !state.selectedCategoryId.isNullOrBlank() -> {
                    state.allCategories.firstOrNull { it.id == state.selectedCategoryId }?.nome
                        ?: "Categoria"
                }
                !state.selectedParentId.isNullOrBlank() -> {
                    state.sideMenuSections.firstOrNull { it.parentId == state.selectedParentId }?.title
                        ?: "Catalogo"
                }
                else -> "Catalogo"
            }
            CatalogHeader(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                onRefresh = onRefresh,
                selectedTitle = headerTitle
            )
            Spacer(modifier = Modifier.height(2.dp))
            TopActionRow(
                cartItemsCount = state.cartItemsCount,
                onMenuClick = onMenuClick,
                onFavoritesClick = onFavoritesClick,
                onCartClick = onCartClick,
                onHistoryClick = onHistoryClick
            )
            //Spacer(modifier = Modifier.height(12.dp))
            // CategorySection removed (categories already available via chips)
            //Spacer(modifier = Modifier.height(8.dp))
            FilterRow(
                onlyOffers = state.onlyOffers,
                availabilityFilter = state.availabilityFilter,
                onToggleOffers = onToggleOffers,
                onAvailabilityFilterChange = onAvailabilityFilterChange,
                allTags = state.allAvailableTags, // <-- usa i tag parsati dal ViewModel
                selectedTags = state.selectedTags,
                onTagToggle = onTagToggle
            )
            Spacer(modifier = Modifier.height(4.dp))

            CatalogContent(
                state = state,
                cartQuantities = state.cart,
                favoriteIds = state.favoriteProductIds,
                onBookmark = onBookmark,
                onAddToCart = onAddToCart,
                onDecreaseCartItem = onDecreaseCartItem,
                onProductClick = onProductClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        state.selectedProduct?.let { selected ->
            ProductDetailDialog(
                product = selected,
                isFavorite = state.favoriteProductIds.contains(selected.id),
                onDismiss = { onProductClick(null) },
                onAddToCart = { onAddToCart(selected.id) },
                onToggleFavorite = { onBookmark(selected.id) }
            )
        }
    }
}

@Composable
private fun CatalogContent(
    state: CatalogUiState,
    cartQuantities: Map<String, Int>,
    favoriteIds: Set<String>,
    onBookmark: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    onDecreaseCartItem: (String) -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.errorMessage != null -> ErrorState(state.errorMessage)

            state.visibleProducts.isEmpty() -> EmptyState(state.searchQuery)

            else -> CatalogList(
                products = state.visibleProducts,
                cartQuantities = cartQuantities,
                favoriteIds = favoriteIds,
                onBookmark = onBookmark,
                onAddToCart = onAddToCart,
                onDecreaseFromCart = onDecreaseCartItem,
                onProductClick = onProductClick,
                listState = listState
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    quantityInCart: Int,
    isFavorite: Boolean,
    onBookmark: () -> Unit,
    onAddToCart: () -> Unit,
    onDecreaseFromCart: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val imageUrl = product.assetImageUrl()
            val context = LocalContext.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
                if (product.isOutOfStock) {
                    AvailabilityBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
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
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Preferiti",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                QuantityStepper(
                    quantity = quantityInCart,
                    onIncrease = onAddToCart,
                    onDecrease = onDecreaseFromCart,
                    canDecrease = quantityInCart > 0
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean = true
) {
    val containerColor = if (quantity > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "Riduci quantita'",
                tint = if (canDecrease) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        IconButton(
            onClick = onIncrease,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Aumenta quantita'")
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
            text = formatPrice(product.price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        product.oldPrice?.let {
            Text(
                text = formatPrice(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
        if (!product.tags.isNullOrEmpty()) {
            val tags = product.tagsList
            val firstTag = tags.firstOrNull().orEmpty()
            val extraCount = (tags.size - 1).coerceAtLeast(0)
            if (firstTag.isNotEmpty()) {
                TagChip(firstTag, MaterialTheme.colorScheme.secondaryContainer)
            }
            if (extraCount > 0) {
                TagChip("+$extraCount", MaterialTheme.colorScheme.tertiaryContainer)
            }
        }
    }
}

@Composable
private fun TagChip(text: String, background: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailDialog(
    product: Product,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 320.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(product.assetImageUrl())
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                        error = painterResource(id = R.drawable.ic_launcher_foreground)
                    )
                    if (product.isOutOfStock) {
                        AvailabilityBadge(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Chiudi dettaglio")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            product.brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = formatPrice(product.price),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            product.oldPrice?.let {
                                Text(
                                    text = formatPrice(it),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = product.availability,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (product.isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (product.isOutOfStock) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    ProductDetailRow(
                        label = "Categoria",
                        value = product.categoryName ?: product.categoryId
                    )
                    ProductDetailRow(
                        label = "Disponibilità in negozio",
                        value = if (product.catalogQuantity > 0) "${product.catalogQuantity} pezzi" else "Non disponibile"
                    )

                    product.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Descrizione",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(desc, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (product.tagsList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tag",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                product.tagsList.forEach { tag ->
                                    TagChip(tag)
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Chiudi",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = {
                                onAddToCart()
                                onDismiss()
                            },
                            enabled = !product.isOutOfStock,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Aggiungi",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AvailabilityBadge(modifier: Modifier = Modifier) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = Color.White
    ) {
        Text("Non disponibile", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CartPanel(
    cartItems: List<CartItemUi>,
    cartItemsCount: Int,
    total: Double,
    isSubmittingOrder: Boolean,
    orderError: String?,
    lastOrderId: Int?,
    deliveryMethod: DeliveryMethod,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSubmitOrder: () -> Unit,
    onDeliveryMethodChange: (DeliveryMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = if (isDark) Color.Black else Color(0xFFFBFBFC),
        contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Column {
                    Text("Carrello", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$cartItemsCount articoli", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Non hai ancora aggiunto prodotti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(cartItems, key = { it.product.catalogId }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrease = { onIncrease(item.product.id) },
                            onDecrease = { onDecrease(item.product.id) },
                            onRemove = { onRemove(item.product.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Metodo di consegna",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(DeliveryMethod.LOCKER, DeliveryMethod.DOMICILIO).forEach { method ->
                    val selected = method == deliveryMethod
                    OutlinedButton(
                        onClick = { onDeliveryMethodChange(method) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            if (method == DeliveryMethod.LOCKER) "Locker" else "Domicilio",
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Totale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatPrice(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            orderError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            lastOrderId?.let {
                Text("Ordine #$it inviato", color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = onSubmitOrder,
                enabled = cartItems.isNotEmpty() && !isSubmittingOrder,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmittingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSubmittingOrder) "Invio..." else "Procedi all'ordine")
            }
        }
    }
}



@Composable
private fun CartItemRow(
    item: CartItemUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = item.product.assetImageUrl()

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatPrice(item.product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRemove) {
                Text("Rimuovi")
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        QuantityStepper(
            quantity = item.quantity,
            onIncrease = onIncrease,
            onDecrease = onDecrease
        )
    }
}

@Composable
private fun EmptyState(query: String, filteredByCategory: Boolean = false) {
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

@Composable
private fun CatalogList(
    products: List<Product>,
    cartQuantities: Map<String, Int>,
    favoriteIds: Set<String>,
    onBookmark: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    onDecreaseFromCart: (String) -> Unit,
    onProductClick: (String) -> Unit,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.catalogId }) { product ->
            val quantity = cartQuantities[product.id] ?: 0
            val isFavorite = favoriteIds.contains(product.id)
            ProductCard(
                product = product,
                quantityInCart = quantity,
                isFavorite = isFavorite,
                onBookmark = { onBookmark(product.id) },
                onAddToCart = { onAddToCart(product.id) },
                onDecreaseFromCart = { onDecreaseFromCart(product.id) },
                onClick = { onProductClick(product.id) }
            )
        }
    }
}

@Composable
private fun FilterRow(
    onlyOffers: Boolean,
    availabilityFilter: AvailabilityFilter,
    onToggleOffers: () -> Unit,
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit,
    allTags: List<String> = emptyList(),
    selectedTags: Set<String> = emptySet(),
    onTagToggle: (String) -> Unit = {}
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

        // render tag chips after availability filters
        if (allTags.isNotEmpty()) {
            items(allTags) { tag ->
                val selected = tag in selectedTags
                AssistChip(
                    onClick = { onTagToggle(tag) },
                    label = { Text(tag) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}


@Composable
private fun CatalogHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    selectedTitle: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna catalogo", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aggiorna")
            }
        }
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

@Composable
private fun TopActionRow(
    cartItemsCount: Int,
    onMenuClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCartClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onMenuClick, shape = RoundedCornerShape(50)) {
                Icon(Icons.Filled.Menu, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Categorie")
            }
            // Removed Storico ordini button (now handled via navbar)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FavoritesActionButton(onClick = onFavoritesClick)
            CartActionButton(count = cartItemsCount, onClick = onCartClick)
        }
    }
}

@Composable
private fun CartActionButton(
    count: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge { Text("$count") }
                }
            }
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = "Apri carrello")
        }
    }
}

@Composable
private fun FavoritesPanel(
    favorites: List<Product>,
    cartQuantities: Map<String, Int>,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Preferiti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${favorites.size} prodotti", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (favorites.isEmpty()) {
                Text(
                    "Non hai ancora aggiunto preferiti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favorites, key = { it.id }) { product ->
                        val qty = cartQuantities[product.id] ?: 0
                        FavoriteItemRow(
                            product = product,
                            quantity = qty,
                            onIncrease = { onIncrease(product.id) },
                            onDecrease = { onDecrease(product.id) },
                            onToggleFavorite = { onToggleFavorite(product.id) },
                            onProductClick = { onProductClick(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteItemRow(
    product: Product,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onToggleFavorite: () -> Unit,
    onProductClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = product.assetImageUrl()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onProductClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                error = painterResource(id = R.drawable.ic_launcher_foreground)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatPrice(product.price), style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(Icons.Rounded.Favorite, contentDescription = "Rimuovi preferito", tint = MaterialTheme.colorScheme.error)
        }
        QuantityStepper(
            quantity = quantity,
            onIncrease = onIncrease,
            onDecrease = onDecrease,
            canDecrease = quantity > 0
        )
    }
}

@Composable
private fun FavoritesActionButton(
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(Icons.Rounded.Favorite, contentDescription = "Apri preferiti")
    }
}

@Composable
private fun CartOverlay(
    onDismiss: () -> Unit,
    cartItems: List<CartItemUi>,
    cartItemsCount: Int,
    total: Double,
    isSubmittingOrder: Boolean,
    orderError: String?,
    lastOrderId: Int?,
    deliveryMethod: DeliveryMethod,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSubmitOrder: () -> Unit,
    onDeliveryMethodChange: (DeliveryMethod) -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        CartPanel(
            cartItems = cartItems,
            cartItemsCount = cartItemsCount,
            total = total,
            isSubmittingOrder = isSubmittingOrder,
            orderError = orderError,
            lastOrderId = lastOrderId,
            deliveryMethod = deliveryMethod,
            onIncrease = onIncrease,
            onDecrease = onDecrease,
            onRemove = onRemove,
            onSubmitOrder = onSubmitOrder,
            onDeliveryMethodChange = onDeliveryMethodChange,
            modifier = Modifier
                .align(Alignment.Center) // centra sia orizz. che vert.
                .padding(16.dp)
                .heightIn(max = 560.dp) // dimensione compatta per il pannello
                .widthIn(max = 360.dp)
        )
    }
}


@Composable
private fun OverlayContainer(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Sfondo grigio opaco che copre tutto lo schermo (incluso lo Scaffold)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Gray.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss)
        ) {}
        Box(modifier = Modifier.matchParentSize()) {
            content()
        }
    }
}

@Composable
private fun SideMenu(
    sections: List<SideMenuSection>,
    onParentSelected: (String?) -> Unit,
    onEntrySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val expansionState = remember(sections) {
        mutableStateMapOf<String, Boolean>().apply {
            sections.forEach { put(it.id, false) }
        }
    }

    val isDark = isSystemInDarkTheme()

    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color.Black else MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Categorie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            TextButton(
                onClick = { onEntrySelected(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 2.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "Tutte le categorie",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        items(sections) { section ->
            val expanded = expansionState[section.id] == true
            SideAccordion(
                section = section,
                expanded = expanded,
                onToggle = {
                    expansionState[section.id] = !expanded
                    onParentSelected(section.parentId)
                },
                onEntrySelected = onEntrySelected
            )
        }
    }
}

@Composable
private fun SideAccordion(
    section: SideMenuSection,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEntrySelected: (String?) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface)
    ) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = section.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                section.entries.forEach { entry ->
                    TextButton(
                        onClick = { onEntrySelected(entry.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

data class SideMenuSection(
    val id: String,
    val parentId: String?,
    val title: String,
    val entries: List<SideMenuEntry>
)

data class SideMenuEntry(
    val id: String?,
    val title: String
)

private enum class CustomerOrderFilter { ACTIVE, COMPLETED }

// La lista supermarketSideMenu è stata rimossa: ora le categorie vengono caricate dal DB
// tramite CatalogViewModel.sideMenuSections

private fun formatPrice(value: Double): String = "\u20AC ${String.format(java.util.Locale.ROOT, "%.2f", value)}"

private fun orderIsFinal(status: String): Boolean = when (status.uppercase()) {
    "CONCLUSO", "CONSEGNATO", "ANNULLATO" -> true
    else -> false
}

private fun orderStatusLabel(status: String, method: String? = null): String = when {
    method.equals("LOCKER", true) && status.equals("SPEDITO", true) -> "Da ritirare"
    method.equals("DOMICILIO", true) && status.equals("SPEDITO", true) -> "In consegna"
    status.uppercase() == "IN_PREPARAZIONE" -> "In preparazione"
    status.uppercase() == "CONCLUSO" -> "Concluso"
    status.uppercase() == "CREATO" -> "Creato"
    status.uppercase() == "SPEDITO" -> "Spedito"
    status.uppercase() == "CONSEGNATO" -> "Consegnato"
    status.uppercase() == "ANNULLATO" -> "Annullato"
    else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun deliveryMethodLabel(method: String): String =
    if (method.equals("DOMICILIO", ignoreCase = true)) "Spesa a domicilio" else "Ritiro nel locker"

private fun formatOrderDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return raw
        .replace('T', ' ')
        .replace("Z", "")
        .trim()
}

/**
 * Public overlay wrapper so other parts (e.g. MainActivity) can open the side menu above the whole UI.
 */
@Composable
fun SideMenuOverlay(
    onDismiss: () -> Unit,
    sections: List<SideMenuSection>,
    onParentSelected: (String?) -> Unit,
    onEntrySelected: (String?) -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            SideMenu(
                sections = sections,
                onParentSelected = onParentSelected,
                onEntrySelected = onEntrySelected,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .heightIn(max = 1000.dp)
                    .widthIn(max = 360.dp)
            )
        }
    }
}

/**
 * Overlay pubblico per il carrello usato da MainActivity
 */
@Composable
fun AppCartOverlay(
    onDismiss: () -> Unit,
    cartItems: List<CartItemUi>,
    cartItemsCount: Int,
    total: Double,
    isSubmittingOrder: Boolean,
    orderError: String?,
    lastOrderId: Int?,
    deliveryMethod: DeliveryMethod,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSubmitOrder: () -> Unit,
    onDeliveryMethodChange: (DeliveryMethod) -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            CartPanel(
                cartItems = cartItems,
                cartItemsCount = cartItemsCount,
                total = total,
                isSubmittingOrder = isSubmittingOrder,
                orderError = orderError,
                lastOrderId = lastOrderId,
                deliveryMethod = deliveryMethod,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onRemove = onRemove,
                onSubmitOrder = onSubmitOrder,
                onDeliveryMethodChange = onDeliveryMethodChange,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .heightIn(max = 560.dp)
                    .widthIn(max = 360.dp)
            )
        }
    }
}

@Composable
fun AppFavoritesOverlay(
    onDismiss: () -> Unit,
    favorites: List<Product>,
    cartQuantities: Map<String, Int>,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onProductClick: (String) -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            FavoritesPanel(
                favorites = favorites,
                cartQuantities = cartQuantities,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onToggleFavorite = onToggleFavorite,
                onProductClick = onProductClick,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .heightIn(max = 560.dp)
                    .widthIn(max = 400.dp)
            )
        }
    }
}


@Composable
fun OrderHistoryPanel(
    orders: List<CustomerOrderHistoryEntry>,
    isLoading: Boolean,
    error: String?,
    pickupInProgressId: Int?,
    pickupMessage: String?,
    onRefresh: () -> Unit,
    onScanQr: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDismissMessage: () -> Unit = {}
) {
    var selectedFilter by rememberSaveable { mutableStateOf(CustomerOrderFilter.ACTIVE) }
    var expandedOrderId by rememberSaveable { mutableStateOf<Int?>(null) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    val filteredOrders = when (selectedFilter) {
        CustomerOrderFilter.ACTIVE -> orders.filter { !orderIsFinal(it.order.stato) }
        CustomerOrderFilter.COMPLETED -> orders.filter { orderIsFinal(it.order.stato) }
    }
    val activeCount = orders.count { !orderIsFinal(it.order.stato) }
    val completedCount = orders.size - activeCount
    val pageSize = 3
    val pageCount = ((filteredOrders.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    currentPage = currentPage.coerceIn(0, pageCount - 1)
    val pageOrders = filteredOrders.drop(currentPage * pageSize).take(pageSize)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Storico ordini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Aggiorna") }
        }

        CustomerOrderFilterChips(
            selected = selectedFilter,
            onSelect = {
                selectedFilter = it
                currentPage = 0
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Attivi: $activeCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Conclusi: $completedCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        pickupMessage?.let {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(it, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onDismissMessage, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Ok") }
            }
        }

        when {
            pageOrders.isEmpty() && error == null && !isLoading -> {
                Text(
                    "Nessun ordine in questa sezione",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pageOrders, key = { it.order.idOrdine }) { entry ->
                        val isExpanded = expandedOrderId == entry.order.idOrdine
                        CustomerOrderCard(
                            entry = entry,
                            isExpanded = isExpanded,
                            pickupInProgressId = pickupInProgressId,
                            onToggleExpand = {
                                expandedOrderId = if (isExpanded) null else entry.order.idOrdine
                            },
                            onScanQr = onScanQr
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pagina ${currentPage + 1} / $pageCount", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = { if (currentPage > 0) currentPage -= 1 },
                    enabled = currentPage > 0
                ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Pagina precedente") }
                FilledTonalIconButton(
                    onClick = { if (currentPage < pageCount - 1) currentPage += 1 },
                    enabled = currentPage < pageCount - 1
                ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Pagina successiva") }
            }
        }
    }
}

@Composable
private fun CustomerOrderCard(
    entry: CustomerOrderHistoryEntry,
    isExpanded: Boolean,
    pickupInProgressId: Int?,
    onToggleExpand: () -> Unit,
    onScanQr: (Int) -> Unit
) {
    val order = entry.order
    val statusLabel = orderStatusLabel(order.stato, order.metodoConsegna)
    val deliveryLabel = deliveryMethodLabel(order.metodoConsegna)
    val readyForPickup = order.metodoConsegna.equals("LOCKER", true) && order.stato.equals("SPEDITO", true)
    val isScanning = pickupInProgressId == order.idOrdine
    var showQr by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() },
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ordine #${entry.sequenceNumber}", fontWeight = FontWeight.SemiBold)
                    Text("Data: ${formatOrderDate(order.dataOrdine)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                "Totale: ${formatPrice(order.totale)} • Articoli: ${order.righe.sumOf { it.quantita }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Consegna: $deliveryLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isExpanded) {
                if (order.metodoConsegna.equals("DOMICILIO", true)) {
                    order.indirizzoSpedizione?.takeIf { it.isNotBlank() }?.let {
                        Text("Indirizzo: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    order.idLocker?.let {
                        Text("Locker: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    order.codiceRitiro?.let {
                        OutlinedButton(
                            onClick = { showQr = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Mostra QR")
                        }
                    }
                }

                HorizontalDivider()

                if (order.righe.isEmpty()) {
                    Text("Dettaglio prodotti non disponibile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        order.righe.forEach { line ->
                            Text("${line.nomeProdotto} x${line.quantita} - ${formatPrice(line.prezzoTotale)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (readyForPickup) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isScanning) "Ritiro in corso..." else "Pronto al locker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { onScanQr(order.idOrdine) },
                            enabled = !isScanning,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isScanning) "Attendi" else "Ritira")
                        }
                    }
                }
            } else {
                Text("Tocca per vedere i dettagli", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showQr && order.codiceRitiro != null) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton = {
                TextButton(onClick = { showQr = false }) { Text("Chiudi") }
            },
            title = { Text("QR locker") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mostra questo codice al locker per il ritiro", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    QrPreviewBox(code = order.codiceRitiro)
                }
            }
        )
    }
}

@Composable
private fun QrPreviewBox(code: String, modifier: Modifier = Modifier) {
    val qrBitmap = remember(code) { buildQrBitmap(code) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR $code",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text(code, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun buildQrBitmap(content: String, size: Int = 512): Bitmap? = try {
    val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    bmp
} catch (_: Exception) {
    null
}

@Composable
private fun CustomerOrderFilterChips(
    selected: CustomerOrderFilter,
    onSelect: (CustomerOrderFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomerOrderFilter.entries.forEach { filter ->
            val selectedState = filter == selected
            AssistChip(
                onClick = { onSelect(filter) },
                label = { Text(if (filter == CustomerOrderFilter.ACTIVE) "In corso" else "Conclusi") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedState) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = if (selectedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun AppOrderHistoryOverlay(
    onDismiss: () -> Unit,
    orders: List<CustomerOrderHistoryEntry>,
    isLoading: Boolean,
    error: String?,
    pickupInProgressId: Int?,
    pickupMessage: String?,
    onRefresh: () -> Unit,
    onScanQr: (Int) -> Unit,
    onDismissMessage: () -> Unit = {}
) {
    OverlayContainer(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrderHistoryPanel(
                orders = orders,
                isLoading = isLoading,
                error = error,
                pickupInProgressId = pickupInProgressId,
                pickupMessage = pickupMessage,
                onRefresh = onRefresh,
                onScanQr = onScanQr,
                onDismissMessage = onDismissMessage,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .heightIn(max = 580.dp)
                    .widthIn(max = 400.dp)
            )
        }
    }
}


@Preview(showBackground = true, widthDp = 1200)
@Composable
private fun CatalogScreenPreview() {
    val products = previewProducts()
    val cartProduct = products.first()
    val sampleState = CatalogUiState(
        visibleProducts = products,
        isLoading = false,
        cart = mapOf(cartProduct.id to 2),
        cartItems = listOf(CartItemUi(cartProduct, 2)),
        cartItemsCount = 2,
        cartTotal = cartProduct.price * 2
    )
    SmartShopMobileTheme {
        CatalogScreen(state = sampleState)
    }
}

@Suppress("MagicNumber")
private fun previewProducts(): List<Product> = listOf(
    Product(
        catalogId = 1001,
        id = "prev-1",
        name = "Succo ACE",
        brand = "Freshly",
        categoryId = "drinks",
        price = 2.59,
        oldPrice = 2.99,
        availability = "disponibile",
        tags = listOf("Offerta")
    ),
    Product(
        catalogId = 1002,
        id = "prev-2",
        name = "Detersivo Piatti",
        brand = "CleanUp",
        categoryId = "cleaning",
        price = 1.99,
        availability = "quasi finito",
        tags = listOf("Formato max")
    ),
    Product(
        catalogId = 1003,
        id = "prev-3",
        name = "Pasta Integrale",
        brand = "GranDuro",
        categoryId = "pasta",
        price = 1.29,
        availability = "disponibile",
        tags = listOf("Dispensa")
    )
)

