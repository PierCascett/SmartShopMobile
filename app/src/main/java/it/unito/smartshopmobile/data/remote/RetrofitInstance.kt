package it.unito.smartshopmobile.data.remote

import it.unito.smartshopmobile.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private data class BackendConfig(val host: String, val port: String) {
        val baseUrl: String get() = "http://$host:$port/api/"
        val assetBaseUrl: String get() = "http://$host:$port/"
    }

    // Inizia con un placeholder, poi rileva automaticamente il server
    private val configRef = AtomicReference(
        BackendConfig(
            host = "192.168.1.1", // Placeholder temporaneo
            port = "3000"
        )
    )

    init {
        println("🔧 Rilevamento automatico del server in corso...")
        // Avvia il discovery in background
        CoroutineScope(Dispatchers.IO).launch {
            val detectedHost = NetworkUtils.detectBackendHost()
            configRef.set(BackendConfig(detectedHost, "3000"))
            println("🔧 ✅ Server rilevato automaticamente: http://$detectedHost:3000")
        }
    }

    val assetBaseUrl: String
        get() = configRef.get().assetBaseUrl

    fun overrideBackend(host: String?, port: String?) {
        val normalizedHost = host?.takeIf { it.isNotBlank() } ?: configRef.get().host
        val normalizedPort = port?.takeIf { it.isNotBlank() } ?: configRef.get().port
        configRef.set(BackendConfig(normalizedHost, normalizedPort))
        println("🔧 Backend updated to: http://$normalizedHost:$normalizedPort")
    }

    fun currentBaseUrl(): String = configRef.get().baseUrl

    fun buildAssetUrl(relativePath: String?): String? {
        if (relativePath.isNullOrBlank()) return null
        val path = relativePath.removePrefix("/")
        return assetBaseUrl + path
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val dynamicBackendInterceptor = Interceptor { chain ->
        val current = configRef.get()
        val safePort = current.port.toIntOrNull() ?: 3000
        val newUrl = chain.request().url
            .newBuilder()
            .scheme("http")
            .host(current.host)
            .port(safePort)
            .build()
        chain.proceed(chain.request().newBuilder().url(newUrl).build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicBackendInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(configRef.get().baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: SmartShopApiService by lazy {
        retrofit.create(SmartShopApiService::class.java)
    }
}


