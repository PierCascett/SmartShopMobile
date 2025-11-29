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
 * Converters per Room: converte liste di stringhe in JSON e viceversa.
 */
class Converters {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromJsonToList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return try {
            gson.fromJson<List<String>>(json, listType)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromListToJson(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }
}

