package it.unito.smartshopmobile.data.repository

import it.unito.smartshopmobile.data.model.Product
import it.unito.smartshopmobile.data.model.ProductAvailability
import it.unito.smartshopmobile.data.model.ProductCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Da fare con con Room + Retrofit.
 */
interface CatalogRepository {
    fun observeCatalog(): Flow<List<Product>>
}

class FakeCatalogRepository : CatalogRepository {
    private val catalogFlow = MutableStateFlow(sampleProducts())

    override fun observeCatalog(): Flow<List<Product>> = catalogFlow.asStateFlow()

    @Suppress("MagicNumber")
    private fun sampleProducts(): List<Product> = listOf(
        Product(
            id = "prd-01",
            name = "Mele Fuji",
            brand = "Bio Valley",
            category = ProductCategory.FRUIT,
            price = 2.49,
            oldPrice = 2.99,
            availability = ProductAvailability.AVAILABLE,
            tags = listOf("Bio", "Dolci"),
            rating = 4.6,
            reviewsCount = 112
        ),
        Product(
            id = "prd-02",
            name = "Pasta Integrale",
            brand = "GranPasta",
            category = ProductCategory.PASTA,
            price = 1.39,
            availability = ProductAvailability.RUNNING_LOW,
            tags = listOf("Integrale"),
            rating = 4.4,
            reviewsCount = 87
        ),
        Product(
            id = "prd-03",
            name = "Detersivo Lavatrice Fresh",
            brand = "CleanUp",
            category = ProductCategory.CLEANING,
            price = 6.99,
            oldPrice = 8.50,
            availability = ProductAvailability.AVAILABLE,
            tags = listOf("Formato famiglia"),
            rating = 4.8,
            reviewsCount = 214
        ),
        Product(
            id = "prd-04",
            name = "Bevanda Avena",
            brand = "GreenChoice",
            category = ProductCategory.DRINKS,
            price = 2.79,
            availability = ProductAvailability.OUT_OF_STOCK,
            tags = listOf("Veg", "Senza lattosio"),
            rating = 4.3,
            reviewsCount = 54
        ),
        Product(
            id = "prd-05",
            name = "Insalata Mista",
            brand = "OrtoExpress",
            category = ProductCategory.VEGETABLES,
            price = 1.99,
            availability = ProductAvailability.AVAILABLE,
            tags = listOf("Pronta da condire"),
            rating = 4.1,
            reviewsCount = 36
        ),
        Product(
            id = "prd-06",
            name = "Detergente Multiuso",
            brand = "Splendit",
            category = ProductCategory.HOME,
            price = 3.49,
            availability = ProductAvailability.RUNNING_LOW,
            tags = listOf("Antibatterico"),
            rating = 4.7,
            reviewsCount = 149
        )
    )

    suspend fun refreshFromNetwork() {
        // simulazione network: quando arrivera' Retrofit qui verra' chiamata l'API catalogo
        delay(400)
    }
}
