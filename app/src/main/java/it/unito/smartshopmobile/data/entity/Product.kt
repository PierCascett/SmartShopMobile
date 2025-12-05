/**
 * Product.kt
 *
 * MVVM: Model Layer - Entity prodotto (Room + API)
 *
 * FUNZIONAMENTO:
 * - Rappresenta prodotto catalogo con quantità scaffale/magazzino
 * - catalogId: chiave primaria (riga univoca per posizione)
 * - id: ID prodotto logico (stesso prodotto può stare su più scaffali)
 * - ForeignKey a Category per integrità referenziale
 *
 * PATTERN MVVM:
 * - Entity: mapping database/API
 * - @Entity: Room table definition
 * - Foreign Keys: relazioni tra tabelle
 * - Indici: performance query su categoria_id e id
 *
 * CAMPI PRINCIPALI:
 * - catalogId: PK univoca per riga catalogo
 * - id: ID prodotto logico (può ripetersi)
 * - catalogQuantity: quantità in scaffale
 * - warehouseQuantity: quantità in magazzino
 * - totalQuantity: somma totale disponibile
 * - price/oldPrice: prezzi (vecchio prezzo per sconti)
 * - shelfId: riferimento scaffale fisico
 */
package it.unito.smartshopmobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Room che rappresenta un prodotto nel catalogo SmartShop.
 *
 * Questa entità gestisce sia i dati provenienti dall'API backend che
 * la persistenza locale tramite Room Database. Supporta la gestione
 * di inventario multi-livello (scaffale + magazzino) e traccia le
 * informazioni di categoria e prezzi con supporto per sconti.
 *
 * Caratteristiche principali:
 * - Chiave primaria univoca per riga catalogo (catalogId)
 * - ID prodotto logico (id) che può ripetersi su più scaffali
 * - Foreign key verso Category per integrità referenziale
 * - Indici su categoria_id e id per ottimizzare le query
 * - Supporto per immagini, tag e prezzi scontati
 *
 * @property catalogId Chiave primaria univoca per questa riga di catalogo
 * @property id ID logico del prodotto (può ripetersi su scaffali diversi)
 * @property name Nome commerciale del prodotto
 * @property brand Marca/produttore del prodotto
 * @property categoryId ID della categoria di appartenenza
 * @property categoryName Nome della categoria (denormalizzato per performance)
 * @property categoryDescription Descrizione della categoria (opzionale)
 * @property catalogQuantity Quantità disponibile sullo scaffale
 * @property warehouseQuantity Quantità disponibile in magazzino
 * @property totalQuantity Quantità totale disponibile (scaffale + magazzino)
 * @property price Prezzo corrente di vendita
 * @property oldPrice Prezzo precedente (usato per mostrare sconti)
 * @property availability Stato disponibilità (es. "OK", "ESAURITO")
 * @property tags Lista di tag per categorizzazione aggiuntiva
 * @property description Descrizione dettagliata del prodotto
 * @property imageUrl URL dell'immagine del prodotto
 * @property shelfId ID dello scaffale fisico dove si trova il prodotto
 */
@Entity(
    tableName = "prodotti_catalogo",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoria_id"]),
        Index(value = ["id"]) // indice su productId per ricerche veloci
    ]
)
data class Product(
    // Chiave primaria: riga di catalogo (univoca per posizione/scaffale)
    @PrimaryKey
    @ColumnInfo(name = "catalog_id")
    val catalogId: Int,

    // ID prodotto logico (può ripetersi su più righe catalogo)
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nome")
    val name: String,

    @ColumnInfo(name = "marca")
    val brand: String,

    @ColumnInfo(name = "categoria_id")
    val categoryId: String,

    @ColumnInfo(name = "categoria_nome")
    val categoryName: String? = null,

    @ColumnInfo(name = "categoria_descrizione")
    val categoryDescription: String? = null,

    @ColumnInfo(name = "catalog_quantity")
    val catalogQuantity: Int = 0,

    @ColumnInfo(name = "warehouse_quantity")
    val warehouseQuantity: Int = 0,

    @ColumnInfo(name = "total_quantity")
    val totalQuantity: Int = 0,

    @ColumnInfo(name = "prezzo")
    val price: Double,

    @ColumnInfo(name = "vecchio_prezzo")
    val oldPrice: Double? = null,

    @ColumnInfo(name = "disponibilita")
    val availability: String,

    @ColumnInfo(name = "tag")
    val tags: List<String>? = null,

    @ColumnInfo(name = "descrizione")
    val description: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    // id scaffale da catalogo (se disponibile nel payload)
    @ColumnInfo(name = "id_scaffale")
    val shelfId: Int? = null
)
