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


import it.unito.smartshopmobile.data.remote.RetrofitInstance.api
import it.unito.smartshopmobile.data.remote.RetrofitInstance.assetBaseUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object per la configurazione Retrofit e l'istanza API.
 *
 * Fornisce un'istanza configurata di SmartShopApiService con:
 * - OkHttpClient con logging interceptor per debug
 * - Gson converter per serializzazione JSON automatica
 * - Timeout configurabili (30s per connect/read/write)
 * - Base URL configurabile per backend Node.js
 *
 * Configurazione:
 * - BACKEND_HOST: IP del server backend (192.168.1.51)
 * - BACKEND_PORT: porta del server backend (3000)
 * - Base URL API: http://HOST:PORT/api/
 * - Asset URL: http://HOST:PORT/ (per immagini)
 *
 * Helper utilities:
 * - buildAssetUrl(): costruisce URL completi per assets statici
 * - currentBaseUrl(): ritorna base URL API corrente
 *
 * Note produzione:
 * - Rimuovere/ridurre logging interceptor per performance
 * - Usare BuildConfig per host/port configurabili
 * - Implementare HTTPS con certificati SSL
 *
 * @property api Istanza singleton lazy-initialized di SmartShopApiService
 * @property assetBaseUrl Base URL per assets statici (immagini, avatar)
 */
object RetrofitInstance {
    private const val BACKEND_HOST = "192.168.1.51"
    private const val BACKEND_PORT = "3000"

    val assetBaseUrl: String
        get() = "http://$BACKEND_HOST:$BACKEND_PORT/"
    /**
     * Helper per gestire current base url.
     */

    fun currentBaseUrl(): String = "${assetBaseUrl}api/"
    /**
     * Helper per gestire build asset url.
     */

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
