/**
 * Restock.kt
 *
 * MVVM: Model Layer - Entity riordino magazzino (Room + API)
 *
 * FUNZIONAMENTO:
 * - Rappresenta riordino merce da fornitore
 * - Stati: ORDINATO, IN_TRANSITO, ARRIVATO
 * - Tracking: data ordine, data arrivo prevista/effettiva
 * - Cache locale per storico manager
 *
 * PATTERN MVVM:
 * - Entity: mapping database/API
 * - @Entity: Room cache table
 * - @SerializedName: mapping JSON
 * - Denormalized: include nomi prodotto/fornitore
 */
package it.unito.smartshopmobile.data.entity


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Entity per riordini magazzino (API + Room cache).
 */
@Entity(tableName = "riordini_cache")
data class Restock(
    @PrimaryKey
    @SerializedName("id_riordino")
    @ColumnInfo(name = "id_riordino")
    val idRiordino: Int,
    @SerializedName("id_prodotto")
    @ColumnInfo(name = "id_prodotto")
    val idProdotto: String,
    @SerializedName("prodotto_nome")
    @ColumnInfo(name = "prodotto_nome")
    val prodottoNome: String,
    @SerializedName("id_fornitore")
    @ColumnInfo(name = "id_fornitore")
    val idFornitore: Int,
    @SerializedName("fornitore_nome")
    @ColumnInfo(name = "fornitore_nome")
    val fornitoreNome: String,
    @SerializedName("quantita_ordinata")
    @ColumnInfo(name = "quantita_ordinata")
    val quantitaOrdinata: Int,
    @SerializedName("data_ordine")
    @ColumnInfo(name = "data_ordine")
    val dataOrdine: String,
    @SerializedName("data_arrivo_prevista")
    @ColumnInfo(name = "data_arrivo_prevista")
    val dataArrivoPrevista: String?,
    @SerializedName("data_arrivo_effettiva")
    @ColumnInfo(name = "data_arrivo_effettiva")
    val dataArrivoEffettiva: String?,
    @SerializedName("arrivato")
    @ColumnInfo(name = "arrivato")
    val arrivato: Boolean,
    @SerializedName("id_responsabile")
    @ColumnInfo(name = "id_responsabile")
    val idResponsabile: Int?,
    @SerializedName("responsabile_nome")
    @ColumnInfo(name = "responsabile_nome")
    val responsabileNome: String?,
    @SerializedName("responsabile_cognome")
    @ColumnInfo(name = "responsabile_cognome")
    val responsabileCognome: String?
)

data class CreateRestockRequest(
    @SerializedName("idProdotto") val idProdotto: String,
    @SerializedName("idFornitore") val idFornitore: Int,
    @SerializedName("quantitaOrdinata") val quantitaOrdinata: Int,
    @SerializedName("dataArrivoPrevista") val dataArrivoPrevista: String?,
    @SerializedName("idResponsabile") val idResponsabile: Int?
)
