/**
 * Converters.kt
 *
 * MVVM: Model Layer - TypeConverters per Room
 *
 * FUNZIONAMENTO:
 * - Converte tipi complessi per persistenza Room
 * - List<String> ↔ JSON string
 * - Usato da Room per serializzare campi non-primitivi
 * - Gson per serializzazione/deserializzazione
 *
 * PATTERN MVVM:
 * - TypeConverter: Room persistenza tipi complessi
 * - @TypeConverter: annotazione Room
 * - Gson: JSON serialization
 * - Error handling: ritorna null se parsing fallisce
 */
package it.unito.smartshopmobile.data.database


import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TypeConverters per Room Database.
 *
 * Fornisce conversioni tra tipi complessi Kotlin e tipi supportati da SQLite.
 * Room non può persistere direttamente tipi come List<String>, quindi questi
 * converter trasformano le liste in JSON e viceversa.
 *
 * Utilizzo automatico:
 * - Annotato con @TypeConverters a livello di Database
 * - Room chiama automaticamente questi metodi durante read/write
 * - Utilizzato principalmente per il campo "tags" in Product
 *
 * Gestione errori:
 * - Parsing fallito → null (evita crash)
 * - Input null/blank → null
 */
class Converters {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    /**
     * Converte una stringa JSON in una lista di stringhe.
     *
     * @param json Stringa JSON da convertire (es. ["tag1","tag2"])
     * @return Lista di stringhe o null se parsing fallisce
     */
    @TypeConverter
    fun fromJsonToList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return try {
            gson.fromJson<List<String>>(json, listType)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converte una lista di stringhe in JSON.
     *
     * @param list Lista di stringhe da serializzare
     * @return Stringa JSON o null se lista è null
     */
    @TypeConverter
    fun fromListToJson(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }
}
