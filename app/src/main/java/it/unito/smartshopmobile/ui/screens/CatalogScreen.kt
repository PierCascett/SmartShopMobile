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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalContext
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
import java.util.concurrent.Executors

// Extension properties per Product entity
val Product.isFavorite: Boolean get() = false // TODO: implementare logica favorites
val Product.isInCart: Boolean get() = false // TODO: implementare da state
val Product.tagsList: List<String> get() = tags ?: emptyList()
val Product.isOutOfStock: Boolean get() = catalogQuantity <= 0

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
            Spacer(modifier = Modifier.height(8.dp))

            CatalogContent(
                state = state,
                cartQuantities = state.cart,
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
                onDismiss = { onProductClick(null) },
                onAddToCart = { onAddToCart(selected.id) }
            )
        }
    }
}

@Composable
private fun CatalogContent(
    state: CatalogUiState,
    cartQuantities: Map<String, Int>,
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
            // Costruisce URL immagine PNG basato sull'ID prodotto
            val imageUrl = "${RetrofitInstance.assetBaseUrl}images/products/${product.id}.png"
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
                        imageVector = if (product.isFavorite) Icons.Rounded.Favorite else Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = if (product.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (product.isInCart) {
                    QuantityStepper(
                        quantity = quantityInCart,
                        onIncrease = onAddToCart,
                        onDecrease = onDecreaseFromCart
                    )
                } else {
                    TextButton(onClick = onAddToCart, shape = RoundedCornerShape(50)) {
                        Text("Aggiungi al carrello")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrease,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Riduci quantita'")
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
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Chiudi") }
                TextButton(onClick = {
                    onAddToCart()
                    onDismiss()
                }) { Text("Aggiungi al carrello") }
            }
        },
        title = {
            Column {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(product.brand, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Categoria: ${product.categoryName ?: product.categoryId}", style = MaterialTheme.typography.bodySmall)
                Text("Prezzo: ${formatPrice(product.price)}", style = MaterialTheme.typography.bodySmall)
                product.oldPrice?.let { Text("Vecchio prezzo: ${formatPrice(it)}", style = MaterialTheme.typography.bodySmall) }
                Text("Disponibilità: ${product.availability}", style = MaterialTheme.typography.bodySmall)
                product.description?.let { desc ->
                    Text(desc, style = MaterialTheme.typography.bodyMedium)
                }
                if (product.tagsList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tag", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            product.tagsList.forEach { tag ->
                                TagChip(tag)
                            }
                        }
                    }
                }
            }
        }
    )
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

    androidx.compose.material3.Surface(
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
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryMethod.values().forEach { method ->
                    val selected = method == deliveryMethod
                    OutlinedButton(
                        onClick = { onDeliveryMethodChange(method) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                        )
                    ) {
                        Text(
                            method.label,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

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
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.primary
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
            ProductCard(
                product = product,
                quantityInCart = quantity,
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
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna catalogo")
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
            TextButton(onClick = onHistoryClick, shape = RoundedCornerShape(50)) {
                Icon(Icons.Filled.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Storico ordini")
            }
        }
        CartActionButton(count = cartItemsCount, onClick = onCartClick)
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

// La lista supermarketSideMenu è stata rimossa: ora le categorie vengono caricate dal DB
// tramite CatalogViewModel.sideMenuSections

private fun formatPrice(value: Double): String = "\u20AC ${String.format(java.util.Locale.ROOT, "%.2f", value)}"

private fun orderStatusLabel(status: String): String = when (status.uppercase()) {
    "IN_PREPARAZIONE" -> "In preparazione"
    "CONCLUSO" -> "Concluso"
    "CREATO" -> "Creato"
    "SPEDITO" -> "Spedito"
    "ANNULLATO" -> "Annullato"
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
private fun OrderHistoryPanel(
    orders: List<CustomerOrderHistoryEntry>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Storico ordini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onRefresh, enabled = !isLoading) {
                    Text("Aggiorna")
                }
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (orders.isEmpty() && error == null && !isLoading) {
                Text(
                    "Nessun ordine effettuato finora",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders, key = { it.order.idOrdine }) { entry ->
                        OrderHistoryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(entry: CustomerOrderHistoryEntry) {
    val order = entry.order
    val statusLabel = orderStatusLabel(order.stato)
    val deliveryLabel = deliveryMethodLabel(order.metodoConsegna)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ordine #${entry.sequenceNumber}", fontWeight = FontWeight.SemiBold)
                Text(statusLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Text("ID backend #${order.idOrdine}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Data: ${formatOrderDate(order.dataOrdine)}", style = MaterialTheme.typography.bodySmall)
            Text("Totale: ${formatPrice(order.totale)}", fontWeight = FontWeight.SemiBold)
            Text("Consegna: $deliveryLabel", style = MaterialTheme.typography.bodySmall)
            if (order.metodoConsegna.equals("LOCKER", ignoreCase = true)) {
                order.idLocker?.let {
                    Text("Locker: $it", style = MaterialTheme.typography.bodySmall)
                }
                order.codiceRitiro?.let {
                    Text("Codice ritiro: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("Consegna a domicilio in gestione", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            if (order.righe.isEmpty()) {
                Text(
                    "Dettaglio prodotti non disponibile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                order.righe.forEach { line ->
                    Text(
                        "${line.nomeProdotto} x${line.quantita} - ${formatPrice(line.prezzoTotale)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun AppOrderHistoryOverlay(
    onDismiss: () -> Unit,
    orders: List<CustomerOrderHistoryEntry>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrderHistoryPanel(
                orders = orders,
                isLoading = isLoading,
                error = error,
                onRefresh = onRefresh,
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

