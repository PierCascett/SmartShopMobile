/**
 * UserRepository.kt
 *
 * MVVM: Model Layer - Repository gestione profilo utente
 *
 * FUNZIONAMENTO:
 * - Aggiorna profilo utente (nome, cognome, email, telefono)
 * - Upload avatar (foto profilo) con multipart
 * - Solo API (no cache, aggiorna SessionDataStore da ViewModel)
 *
 * PATTERN MVVM:
 * - Repository Pattern: astrazione API utente
 * - suspend fun: operazioni asincrone
 * - Result<T>: gestione errori update
 * - Multipart upload per foto profilo
 */
package it.unito.smartshopmobile.data.repository


import android.util.Log
import it.unito.smartshopmobile.data.entity.UpdateUserRequest
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.SmartShopApiService
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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

    suspend fun uploadAvatar(userId: Int, data: ByteArray, mimeType: String): Result<String> {
        return try {
            val body = data.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("photo", "avatar.jpg", body)
            val response = apiService.uploadProfilePhoto(userId, part)
            if (response.isSuccessful) {
                val payload = response.body()
                val url = payload?.get("avatarUrl") ?: payload?.get("avatar_url")
                if (url is String && url.isNotBlank()) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("Risposta upload foto non valida"))
                }
            } else {
                Result.failure(Exception("Upload foto fallito (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore upload avatar", e)
            Result.failure(e)
        }
    }
}
