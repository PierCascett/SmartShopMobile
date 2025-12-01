/**
 * RetrofitInstance.kt
 *
 * MVVM: Model Layer - Configurazione client HTTP (Retrofit)
 *
 * FUNZIONAMENTO:
 * - Singleton Retrofit per chiamate API REST
 * - OkHttp con logging interceptor (debugging)
 * - Gson converter per serializzazione JSON
 * - Timeout configurabili (30s connect/read)
 * - Base URL configurabile (backend Node.js)
 *
 * PATTERN MVVM:
 * - Singleton: unica istanza Retrofit
 * - Retrofit: client HTTP type-safe
 * - OkHttp: interceptor per logging
 * - Gson: JSON ↔ Kotlin objects
 */
package it.unito.smartshopmobile.data.remote


import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BACKEND_HOST = "192.168.1.3"
    private const val BACKEND_PORT = "3000"

    val assetBaseUrl: String
        get() = "http://$BACKEND_HOST:$BACKEND_PORT/"

    fun currentBaseUrl(): String = "${assetBaseUrl}api/"

    fun buildAssetUrl(relativePath: String?): String? {
        if (relativePath.isNullOrBlank()) return null
        val path = relativePath.removePrefix("/")
        return assetBaseUrl + path
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(currentBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: SmartShopApiService by lazy {
        retrofit.create(SmartShopApiService::class.java)
    }
}
