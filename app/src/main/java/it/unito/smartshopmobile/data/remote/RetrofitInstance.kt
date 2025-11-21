package it.unito.smartshopmobile.data.remote

import android.os.Build
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

    // Default: usa 10.0.2.2 per emulatore Android, altrimenti usa IP rete locale
    private val configRef = AtomicReference(
        BackendConfig(
            host = if (isEmulator()) "10.0.2.2" else "192.168.1.51",
            port = "3000"
        )
    )

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

    private fun isEmulator(): Boolean {
        return (
            Build.FINGERPRINT.startsWith("google/sdk_gphone") ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.contains("vbox") ||
                Build.FINGERPRINT.contains("test-keys") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                "google_sdk" == Build.PRODUCT
            )
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


