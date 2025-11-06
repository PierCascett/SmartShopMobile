package it.unito.smartshopmobile.data.model

import androidx.annotation.DrawableRes
import it.unito.smartshopmobile.R

/**
 * Modello di dominio mostrato nella schermata catalogo.
 *
 * Quando verra' agganciato Room+Retrofit questo file potra' essere popolato
 * da mapper che convertono le entita' locali (Room) e le DTO remoti (Retrofit)
 * in Product, cosi' la UI rimane indipendente dalla sorgente dati.
 */
data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val category: ProductCategory,
    val price: Double,
    val oldPrice: Double? = null,
    val availability: ProductAvailability,
    val tags: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val isFavorite: Boolean = false,
    val isInCart: Boolean = false,
    @DrawableRes val imageRes: Int = R.drawable.ic_launcher_foreground
)

enum class ProductAvailability {
    AVAILABLE,
    RUNNING_LOW,
    OUT_OF_STOCK
}

enum class ProductCategory(val label: String) {
    FRUIT("Frutta"),
    VEGETABLES("Verdura"),
    HOME("Casa"),
    CLEANING("Detersivi"),
    PASTA("Pasta"),
    DRINKS("Bevande"),
    OTHER("Altro")
}
