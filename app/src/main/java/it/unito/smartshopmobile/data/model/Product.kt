/**
 * Product.kt
 *
 * RUOLO MVVM: Model Layer (Domain Model)
 * - Rappresenta i dati di business dell'applicazione
 * - Utilizzato da ViewModel e UI (indipendente da Room/Retrofit)
 * - Contiene la logica di dominio e le regole di validazione
 *
 * RESPONSABILITÀ:
 * - Product: classe dati principale per i prodotti del catalogo
 * - ProductCategory: enum per le categorie di prodotti
 * - ProductAvailability: enum per gli stati di disponibilità
 *
 * PATTERN: Domain Model
 * - Separato dalle Entity Room (data/entity/) e DTO Retrofit (futuro)
 * - Mapper convertiranno Entity/DTO → Product per l'UI
 * - Permette di cambiare il database senza toccare UI/ViewModel
 */
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
