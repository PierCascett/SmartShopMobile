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
    @SerializedName("metodo_consegna")
    @ColumnInfo(name = "metodo_consegna")
    val metodoConsegna: String,
    @SerializedName("id_locker")
    @ColumnInfo(name = "id_locker")
    val idLocker: Int?,
    @SerializedName("codice_ritiro")
    @ColumnInfo(name = "codice_ritiro")
    val codiceRitiro: String?,
    @SerializedName("indirizzo_spedizione")
    @ColumnInfo(name = "indirizzo_spedizione")
    val indirizzoSpedizione: String? = null,
    @SerializedName("nome")
    @ColumnInfo(name = "nome_cliente")
    val nomeCliente: String,
    @SerializedName("cognome")
    @ColumnInfo(name = "cognome_cliente")
    val cognomeCliente: String,
    @SerializedName("email")
    @ColumnInfo(name = "email_cliente")
    val emailCliente: String,
    @SerializedName("locker")
    @Ignore
    val locker: LockerInfo? = null,
    @SerializedName("consegnaDomicilio")
    @Ignore
    val consegnaDomicilio: HomeDeliveryInfo? = null,
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
        metodoConsegna: String,
        idLocker: Int?,
        codiceRitiro: String?,
        indirizzoSpedizione: String?,
        nomeCliente: String,
        cognomeCliente: String,
        emailCliente: String
    ) : this(
        idOrdine = idOrdine,
        idUtente = idUtente,
        dataOrdine = dataOrdine,
        stato = stato,
        totale = totale,
        metodoConsegna = metodoConsegna,
        idLocker = idLocker,
        codiceRitiro = codiceRitiro,
        indirizzoSpedizione = indirizzoSpedizione,
        nomeCliente = nomeCliente,
        cognomeCliente = cognomeCliente,
        emailCliente = emailCliente,
        locker = null,
        consegnaDomicilio = null,
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
    @SerializedName("metodoConsegna")
    val metodoConsegna: String,
    @SerializedName("indirizzoSpedizione")
    val indirizzoSpedizione: String?,
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

data class UpdateOrderStatusRequest(
    @SerializedName("stato")
    val stato: String
)

data class LockerInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("codice")
    val codice: String?,
    @SerializedName("posizione")
    val posizione: String?,
    @SerializedName("occupato")
    val occupato: Boolean,
    @SerializedName("codiceRitiro")
    val codiceRitiro: String? = null
)

data class HomeDeliveryInfo(
    @SerializedName("idRider")
    val idRider: Int?,
    @SerializedName("dataAssegnazione")
    val dataAssegnazione: String?,
    @SerializedName("dataConsegna")
    val dataConsegna: String?
)
