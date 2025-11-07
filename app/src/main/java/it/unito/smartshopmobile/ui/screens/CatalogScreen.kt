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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import it.unito.smartshopmobile.viewModel.CartItemUi
import it.unito.smartshopmobile.viewModel.CatalogUiState

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
    onToggleOffers: () -> Unit = {},
    onAvailabilityFilterChange: (AvailabilityFilter) -> Unit = {},
    onTagToggle: (String) -> Unit = {},
    onBookmark: (String) -> Unit = {},
    onAddToCart: (String) -> Unit = {},
    onDecreaseCartItem: (String) -> Unit = {},
    onRemoveFromCart: (String) -> Unit = {}
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showCart by rememberSaveable { mutableStateOf(false) }

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
            CatalogHeader(state.searchQuery, onSearchQueryChange)
            Spacer(modifier = Modifier.height(2.dp))
            TopActionRow(
                cartItemsCount = state.cartItemsCount,
                onMenuClick = { showMenu = true },
                onCartClick = { showCart = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            // CategorySection removed (categories already available via chips)
            Spacer(modifier = Modifier.height(8.dp))
            FilterRow(
                onlyOffers = state.onlyOffers,
                availabilityFilter = state.availabilityFilter,
                onToggleOffers = onToggleOffers,
                onAvailabilityFilterChange = onAvailabilityFilterChange,
                allTags = state.products.flatMap { it.tags }.distinct(),
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        if (showMenu) {
            SideMenuOverlay(
                onDismiss = { showMenu = false },
                sections = supermarketSideMenu,
                onEntrySelected = {
                    showMenu = false
                    onSearchQueryChange(it)
                }
            )
        }

        if (showCart) {
            CartOverlay(
                onDismiss = { showCart = false },
                cartItems = state.cartItems,
                cartItemsCount = state.cartItemsCount,
                total = state.cartTotal,
                onIncrease = onAddToCart,
                onDecrease = onDecreaseCartItem,
                onRemove = onRemoveFromCart
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
    modifier: Modifier = Modifier
) {
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

            state.visibleProducts.isEmpty() -> EmptyState(state.searchQuery, state.selectedCategory != null)

            else -> CatalogList(
                products = state.visibleProducts,
                cartQuantities = cartQuantities,
                onBookmark = onBookmark,
                onAddToCart = onAddToCart,
                onDecreaseFromCart = onDecreaseCartItem
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
    onDecreaseFromCart: () -> Unit
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
private fun CartPanel(
    cartItems: List<CartItemUi>,
    cartItemsCount: Int,
    total: Double,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
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
        Divider(modifier = Modifier.padding(vertical = 12.dp))
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
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { onIncrease(item.product.id) },
                        onDecrease = { onDecrease(item.product.id) },
                        onRemove = { onRemove(item.product.id) }
                    )
                }
            }
        }

        Divider()
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
        Button(
            onClick = { /* CTA da collegare in futuro */ },
            enabled = cartItems.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Procedi all'ordine")
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

@Composable
private fun CatalogList(
    products: List<Product>,
    cartQuantities: Map<String, Int>,
    onBookmark: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    onDecreaseFromCart: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            val quantity = cartQuantities[product.id] ?: 0
            ProductCard(
                product = product,
                quantityInCart = quantity,
                onBookmark = { onBookmark(product.id) },
                onAddToCart = { onAddToCart(product.id) },
                onDecreaseFromCart = { onDecreaseFromCart(product.id) }
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
        items(AvailabilityFilter.entries) { filter ->
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

@Composable
private fun TopActionRow(
    cartItemsCount: Int,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onMenuClick, shape = RoundedCornerShape(50)) {
            Icon(Icons.Filled.Menu, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Categorie")
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
private fun SideMenuOverlay(
    onDismiss: () -> Unit,
    sections: List<SideMenuSection>,
    onEntrySelected: (String) -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        SideMenu(
            sections = sections,
            onEntrySelected = onEntrySelected,
            modifier = Modifier
                .align(Alignment.Center) // centra sia orizz. che vert.
                .padding(16.dp)
                .heightIn(max = 1000.dp) // non occupa tutto lo schermo
                .widthIn(max = 360.dp)
        )
    }
}

@Composable
private fun CartOverlay(
    onDismiss: () -> Unit,
    cartItems: List<CartItemUi>,
    cartItemsCount: Int,
    total: Double,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    OverlayContainer(onDismiss = onDismiss) {
        CartPanel(
            cartItems = cartItems,
            cartItemsCount = cartItemsCount,
            total = total,
            onIncrease = onIncrease,
            onDecrease = onDecrease,
            onRemove = onRemove,
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
        )
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun SideMenu(
    sections: List<SideMenuSection>,
    onEntrySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expansionState = remember(sections) {
        mutableStateMapOf<String, Boolean>().apply {
            sections.forEach { put(it.id, false) }
        }
    }

    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Categorie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(sections) { section ->
            val expanded = expansionState[section.id] == true
            SideAccordion(
                section = section,
                expanded = expanded,
                onToggle = { expansionState[section.id] = !expanded },
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
    onEntrySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
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
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
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
                        onClick = { onEntrySelected(entry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(entry, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private data class SideMenuSection(
    val id: String,
    val title: String,
    val entries: List<String>
)

private val supermarketSideMenu = listOf(
    SideMenuSection(
        id = "meat",
        title = "Carne e Pesce",
        entries = listOf(
            "Pollo e Tacchino",
            "Manzo e Vitello",
            "Maiale",
            "Spiedini, Polpette e Specialita' di Carne",
            "Altre Carni",
            "Wurstel e Carni Impanate",
            "Pesce Fresco",
            "Specialita' di Pesce"
        )
    ),
    SideMenuSection(
        id = "fruitveg",
        title = "Frutta e Verdura",
        entries = listOf(
            "Frutta",
            "Verdura",
            "Bio",
            "In offerta"
        )
    ),
    SideMenuSection(
        id = "dispensa",
        title = "Dispensa",
        entries = listOf(
            "Pasta",
            "Riso e Cereali",
            "Sughi e Condimenti",
            "Snack e Biscotti"
        )
    ),
    SideMenuSection(
        id = "det",
        title = "Detersivi",
        entries = listOf(
            "Piatti",
            "Lavatrice",
            "Casa"
        )
    )
)

private fun formatPrice(value: Double): String = "\u20AC ${String.format(java.util.Locale.ROOT, "%.2f", value)}"

@Preview(showBackground = true, widthDp = 1200)
@Composable
private fun CatalogScreenPreview() {
    val products = previewProducts()
    val cartProduct = products.first()
    val sampleState = CatalogUiState(
        products = products,
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
    ),
    Product(
        id = "prev-3",
        name = "Pasta Integrale",
        brand = "GranDuro",
        category = ProductCategory.PASTA,
        price = 1.29,
        availability = ProductAvailability.AVAILABLE,
        tags = listOf("Dispensa"),
        rating = 4.0,
        reviewsCount = 40
    )
)
