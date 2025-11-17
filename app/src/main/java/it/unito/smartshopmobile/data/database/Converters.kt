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

