/**
 * StockTransfer.kt
 *
 * MVVM: Model Layer - DTO trasferimento stock (API)
 *
 * FUNZIONAMENTO:
 * - StockTransferRequest: richiesta trasferimento magazzino→scaffale
 * - StockTransferResult: risultato con quantità aggiornate
 * - CatalogUpdate: aggiornamento quantità scaffale
 * - Usato da InventoryRepository per API manager
 *
 * PATTERN MVVM:
 * - DTO (Data Transfer Object): comunicazione API
 * - @SerializedName: mapping JSON
 * - Immutabile: data class con val
 * - Non persistito in Room: solo rete
 */
package it.unito.smartshopmobile.data.entity


import com.google.gson.annotations.SerializedName

/**
 * DTO per la richiesta di trasferimento stock da magazzino a scaffale.
 *
 * Il manager può utilizzare questa funzionalità per rifornire gli scaffali
 * quando la merce in magazzino è disponibile ma non ancora esposta.
 *
 * @property idProdotto ID del prodotto da trasferire
 * @property quantita Quantità da spostare dal magazzino allo scaffale
 * @property idScaffale ID dello scaffale di destinazione
 */
data class StockTransferRequest(
    @SerializedName("idProdotto")
    val idProdotto: String,
    @SerializedName("quantita")
    val quantita: Int,
    @SerializedName("idScaffale")
    val idScaffale: Int
)

/**
 * DTO per il risultato di un trasferimento stock dall'API.
 *
 * Contiene le quantità aggiornate dopo il trasferimento e i dettagli
 * della riga di catalogo modificata.
 *
 * @property idProdotto ID del prodotto trasferito
 * @property idScaffale ID dello scaffale di destinazione
 * @property quantitaTrasferita Quantità effettivamente trasferita
 * @property magazzinoResiduo Quantità rimanente in magazzino dopo il trasferimento
 * @property catalogo Dettagli aggiornati della riga di catalogo
 */
data class StockTransferResult(
    @SerializedName("idProdotto")
    val idProdotto: String,
    @SerializedName("idScaffale")
    val idScaffale: Int,
    @SerializedName("quantitaTrasferita")
    val quantitaTrasferita: Int,
    @SerializedName("magazzinoResiduo")
    val magazzinoResiduo: Int,
    @SerializedName("catalogo")
    val catalogo: CatalogUpdate
)

/**
 * DTO contenente i dettagli aggiornati di una riga di catalogo.
 *
 * @property idCatalogo ID della riga di catalogo aggiornata
 * @property quantitaDisponibile Nuova quantità disponibile sullo scaffale
 * @property prezzo Prezzo corrente del prodotto
 * @property vecchioPrezzo Prezzo precedente (per sconti)
 */
data class CatalogUpdate(
    @SerializedName("idCatalogo")
    val idCatalogo: Int,
    @SerializedName("quantitaDisponibile")
    val quantitaDisponibile: Int,
    @SerializedName("prezzo")
    val prezzo: Double,
    @SerializedName("vecchioPrezzo")
    val vecchioPrezzo: Double?
)
