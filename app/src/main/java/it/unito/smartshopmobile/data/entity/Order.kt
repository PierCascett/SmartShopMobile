package it.unito.smartshopmobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.google.gson.annotations.SerializedName

/**
 * Entity per ordini e righe ordine (usata sia per API che per Room).
 */
@Entity(tableName = "ordini_cache")
data class Order(
    @PrimaryKey
    @SerializedName("id_ordine")
    @ColumnInfo(name = "id_ordine")
    val idOrdine: Int,
    @SerializedName("id_utente")
    @ColumnInfo(name = "id_utente")
    val idUtente: Int,
    @SerializedName("data_ordine")
    @ColumnInfo(name = "data_ordine")
    val dataOrdine: String,
    @SerializedName("stato")
    @ColumnInfo(name = "stato")
    val stato: String,
    @SerializedName("totale")
    @ColumnInfo(name = "totale")
    val totale: Double,
    @SerializedName("nome")
    @ColumnInfo(name = "nome_cliente")
    val nomeCliente: String,
    @SerializedName("cognome")
    @ColumnInfo(name = "cognome_cliente")
    val cognomeCliente: String,
    @SerializedName("email")
    @ColumnInfo(name = "email_cliente")
    val emailCliente: String,
    @SerializedName("righe")
    @Ignore
    val righe: List<OrderLine> = emptyList()
) {
    constructor(
        idOrdine: Int,
        idUtente: Int,
        dataOrdine: String,
        stato: String,
        totale: Double,
        nomeCliente: String,
        cognomeCliente: String,
        emailCliente: String
    ) : this(
        idOrdine = idOrdine,
        idUtente = idUtente,
        dataOrdine = dataOrdine,
        stato = stato,
        totale = totale,
        nomeCliente = nomeCliente,
        cognomeCliente = cognomeCliente,
        emailCliente = emailCliente,
        righe = emptyList()
    )
}

@Entity(tableName = "righe_ordine_cache")
data class OrderLine(
    @PrimaryKey
    @SerializedName("id_riga")
    @ColumnInfo(name = "id_riga")
    val idRiga: Int,
    @SerializedName("id_ordine")
    @ColumnInfo(name = "id_ordine")
    val idOrdine: Int,
    @SerializedName("id_prodotto")
    @ColumnInfo(name = "id_prodotto")
    val idProdotto: String,
    @SerializedName("quantita")
    @ColumnInfo(name = "quantita")
    val quantita: Int,
    @SerializedName("prezzo_unitario")
    @ColumnInfo(name = "prezzo_unitario")
    val prezzoUnitario: Double,
    @SerializedName("prezzo_totale")
    @ColumnInfo(name = "prezzo_totale")
    val prezzoTotale: Double,
    @SerializedName("nome")
    @ColumnInfo(name = "nome_prodotto")
    val nomeProdotto: String,
    @SerializedName("marca")
    @ColumnInfo(name = "marca_prodotto")
    val marcaProdotto: String
)

data class OrderWithLines(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "id_ordine",
        entityColumn = "id_ordine"
    )
    val lines: List<OrderLine>
)

data class CreateOrderRequest(
    @SerializedName("idUtente")
    val idUtente: Int,
    @SerializedName("items")
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    @SerializedName("idProdotto")
    val idProdotto: String,
    @SerializedName("quantita")
    val quantita: Int
)

data class OrderCreated(
    @SerializedName("idOrdine")
    val idOrdine: Int,
    @SerializedName("totale")
    val totale: Double
)
