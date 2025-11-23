package it.unito.smartshopmobile.data.entity

import com.google.gson.annotations.SerializedName

data class StockTransferRequest(
    @SerializedName("idProdotto")
    val idProdotto: String,
    @SerializedName("quantita")
    val quantita: Int,
    @SerializedName("idScaffale")
    val idScaffale: Int
)

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
