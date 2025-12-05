/**
 * Shelf.kt
 *
 * MVVM: Model Layer - Entity scaffale supermercato (Room + API)
 *
 * FUNZIONAMENTO:
 * - Rappresenta scaffale fisico del negozio
 * - Usato per mappa interattiva (picking dipendente)
 * - Mapping poligono mappa → scaffale (id corrisponde)
 * - Relazione con Product per localizzazione
 *
 * PATTERN MVVM:
 * - Entity: mapping database/API
 * - @Entity: Room table
 * - @SerializedName: mapping JSON
 * - Immutabile: data class
 */
package it.unito.smartshopmobile.data.entity


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Entity Room che rappresenta uno scaffale fisico del supermercato.
 *
 * Gli scaffali sono utilizzati per:
 * - Visualizzare la mappa interattiva del negozio (employee)
 * - Localizzare prodotti durante il picking degli ordini
 * - Gestire l'inventario (manager può spostare merce da magazzino a scaffale)
 *
 * Caratteristiche principali:
 * - Corrisponde ai poligoni sulla mappa del negozio
 * - Relazione 1-N con Product (un scaffale contiene molti prodotti)
 * - ID numerico progressivo (corrisponde all'ID del poligono mappa)
 * - Descrizione opzionale per dettagli aggiuntivi
 *
 * Esempio:
 * - id: 1, nome: "Scaffale A1", descrizione: "Corsia alimentari - lato sinistro"
 *
 * @property id ID univoco dello scaffale (chiave primaria, corrisponde a polygon ID)
 * @property nome Nome identificativo dello scaffale (es. "A1", "B3")
 * @property descrizione Descrizione testuale opzionale della posizione
 */
@Entity(tableName = "scaffali")
data class Shelf(
    @PrimaryKey
    @SerializedName("id_scaffale")
    @ColumnInfo(name = "id_scaffale")
    val id: Int,
    @SerializedName("nome")
    @ColumnInfo(name = "nome")
    val nome: String,
    @SerializedName("descrizione")
    @ColumnInfo(name = "descrizione")
    val descrizione: String? = null
)

