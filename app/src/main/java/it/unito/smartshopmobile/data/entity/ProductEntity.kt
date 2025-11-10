/**
 * ProductEntity.kt
 *
 * RUOLO MVVM: Entity - Data Layer (Database Schema)
 * - Rappresenta la struttura della tabella "products" nel database Room
 * - Annotata con @Entity per definire schema SQL
 * - Separata dal Model di dominio (Product) per disaccoppiamento
 *
 * RESPONSABILITÀ:
 * - Definire colonne della tabella (@ColumnInfo)
 * - Definire chiavi primarie (@PrimaryKey)
 * - Definire relazioni tra tabelle (@Relation, @ForeignKey)
 * - Converter per tipi complessi (List, Enum) → @TypeConverter
 *
 * PATTERN: Data Transfer Object (DTO) per database
 * - ProductEntity ≠ Product (domain model)
 * - Mapper: ProductEntity.toDomain() → Product
 * - Mapper: Product.toEntity() → ProductEntity
 * - Permette di cambiare lo schema DB senza toccare UI/ViewModel
 *
 * ESEMPIO (futuro):
 * @Entity(tableName = "products")
 * data class ProductEntity(
 *     @PrimaryKey val id: String,
 *     @ColumnInfo(name = "product_name") val name: String,
 *     val price: Double,
 *     val category: String // Enum convertito in String
 * ) {
 *     fun toDomain() = Product(id, name, ...)
 * }
 */
package it.unito.smartshopmobile.data.entity

// ProductEntity - entità Room per la tabella products

