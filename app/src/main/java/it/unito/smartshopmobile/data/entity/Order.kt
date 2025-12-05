/**
 * Order.kt
 *
 * MVVM: Model Layer - Entity ordine cliente (Room + API)
 *
 * FUNZIONAMENTO:
 * - Rappresenta ordine con stato, totale, metodo consegna
 * - Relazione 1-N con OrderLine (righe ordine)
 * - @Ignore righe: non persistito in table Order, caricato con @Relation
 * - Cache locale per offline-first e picking dipendente
 *
 * PATTERN MVVM:
 * - Entity: mapping database/API
 * - @Entity: Room table definition
 * - @SerializedName: mapping JSON API
 * - @Relation: join con OrderLine
 */
package it.unito.smartshopmobile.data.entity


import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.google.gson.annotations.SerializedName

/**
 * Entity Room che rappresenta un ordine cliente nel sistema SmartShop.
 *
 * Questa entità gestisce sia i dati provenienti dall'API backend che
 * la persistenza locale tramite Room Database. Supporta due modalità di
 * consegna: ritiro in locker e consegna a domicilio.
 *
 * Caratteristiche principali:
 * - Cache locale per funzionamento offline-first
 * - Stati ordine: IN_LAVORAZIONE, PRONTO_RITIRO, SPEDITO, CONSEGNATO
 * - Relazione 1-N con OrderLine per le righe dell'ordine
 * - Campi @Ignore per dati non persistiti localmente (locker, consegnaDomicilio, righe)
 * - Supporto completo serializzazione JSON con @SerializedName
 *
 * @property idOrdine ID univoco dell'ordine (chiave primaria)
 * @property idUtente ID del cliente che ha effettuato l'ordine
 * @property dataOrdine Data e ora di creazione ordine (formato ISO 8601)
 * @property stato Stato corrente dell'ordine
 * @property totale Importo totale dell'ordine
 * @property metodoConsegna "LOCKER" o "DOMICILIO"
 * @property idLocker ID del locker assegnato (null se domicilio)
 * @property codiceRitiro Codice PIN per ritiro locker (null se domicilio)
 * @property indirizzoSpedizione Indirizzo consegna a domicilio (null se locker)
 * @property nomeCliente Nome del cliente
 * @property cognomeCliente Cognome del cliente
 * @property emailCliente Email del cliente
 * @property locker Informazioni dettagliate locker (solo da API, non persistito)
 * @property consegnaDomicilio Informazioni consegna (solo da API, non persistito)
 * @property righe Lista righe ordine (solo da API, non persistito qui)
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

/**
 * Entity Room che rappresenta una singola riga (item) di un ordine.
 *
 * Ogni riga contiene le informazioni su un prodotto specifico nell'ordine:
 * quantità, prezzi unitari e totali, e dettagli del prodotto.
 *
 * @property idRiga ID univoco della riga ordine (chiave primaria)
 * @property idOrdine ID dell'ordine padre (foreign key verso Order)
 * @property idProdotto ID del prodotto ordinato
 * @property quantita Quantità ordinata di questo prodotto
 * @property prezzoUnitario Prezzo per singola unità al momento dell'ordine
 * @property prezzoTotale Prezzo totale per questa riga (quantita * prezzoUnitario)
 * @property nomeProdotto Nome del prodotto (denormalizzato)
 * @property marcaProdotto Marca del prodotto (denormalizzato)
 */
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

/**
 * Data class che combina un Order con le sue OrderLine tramite relazione Room.
 *
 * Usata per query che necessitano di ordine completo con tutte le righe.
 * Room popola automaticamente la lista lines tramite @Relation.
 *
 * @property order L'ordine principale
 * @property lines Lista di righe dell'ordine
 */
data class OrderWithLines(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "id_ordine",
        entityColumn = "id_ordine"
    )
    val lines: List<OrderLine>
)

/**
 * DTO per la richiesta di creazione nuovo ordine all'API.
 *
 * @property idUtente ID del cliente che effettua l'ordine
 * @property metodoConsegna "LOCKER" o "DOMICILIO"
 * @property indirizzoSpedizione Indirizzo (richiesto se metodoConsegna = "DOMICILIO")
 * @property items Lista di prodotti e quantità da ordinare
 */
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

/**
 * DTO per un singolo item nella richiesta di creazione ordine.
 *
 * @property idProdotto ID del prodotto da ordinare
 * @property quantita Quantità desiderata
 */
data class OrderItemRequest(
    @SerializedName("idProdotto")
    val idProdotto: String,
    @SerializedName("quantita")
    val quantita: Int
)

/**
 * DTO per la risposta di creazione ordine dall'API.
 *
 * @property idOrdine ID del nuovo ordine creato
 * @property totale Importo totale calcolato dal backend
 */
data class OrderCreated(
    @SerializedName("idOrdine")
    val idOrdine: Int,
    @SerializedName("totale")
    val totale: Double
)

/**
 * DTO per la richiesta di aggiornamento stato ordine.
 *
 * @property stato Nuovo stato (es. "PRONTO_RITIRO", "CONSEGNATO")
 */
data class UpdateOrderStatusRequest(
    @SerializedName("stato")
    val stato: String
)

/**
 * DTO contenente informazioni dettagliate su un locker.
 *
 * @property id ID del locker
 * @property codice Codice identificativo del locker fisico
 * @property posizione Descrizione posizione fisica del locker
 * @property occupato True se il locker è attualmente occupato
 * @property codiceRitiro Codice PIN per il ritiro (generato al momento dell'assegnazione)
 */
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

/**
 * DTO contenente informazioni su una consegna a domicilio.
 *
 * @property idRider ID del rider assegnato alla consegna (null se non ancora assegnato)
 * @property dataAssegnazione Data/ora di assegnazione al rider
 * @property dataConsegna Data/ora di consegna effettiva (null se non ancora consegnato)
 */
data class HomeDeliveryInfo(
    @SerializedName("idRider")
    val idRider: Int?,
    @SerializedName("dataAssegnazione")
    val dataAssegnazione: String?,
    @SerializedName("dataConsegna")
    val dataConsegna: String?
)
