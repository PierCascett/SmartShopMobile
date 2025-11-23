package it.unito.smartshopmobile.data.repository

import android.util.Log
import it.unito.smartshopmobile.data.entity.UpdateUserRequest
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import org.json.JSONObject

class UserRepository(
    private val apiService: SmartShopApiService
    ) {
    suspend fun updateProfile(userId: Int, request: UpdateUserRequest): Result<User> {
        return try {
            val response = apiService.updateProfile(userId, request)
            if (response.isSuccessful) {
                response.body()?.get("user")?.let { Result.success(it) }
                    ?: Result.failure(Exception("Risposta vuota dal server"))
            } else {
                val errorMsg = response.errorBody()?.string()
                val parsed = errorMsg?.let {
                    try {
                        JSONObject(it).optString("error").takeIf { msg -> msg.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    }
                }
                Result.failure(Exception(parsed ?: "Aggiornamento profilo fallito (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore aggiornamento profilo", e)
            Result.failure(e)
        }
    }
}
