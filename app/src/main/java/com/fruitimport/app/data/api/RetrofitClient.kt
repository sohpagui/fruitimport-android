package com.fruitimport.app.data.api

// ============================================================
// FICHIER : data/api/RetrofitClient.kt
// Rôle : Configure Retrofit pour communiquer avec le backend.
//        - Ajoute automatiquement le token JWT à chaque requête
//        - Gère les erreurs réseau
//        - Log les requêtes en mode debug
// ============================================================

import com.fruitimport.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Token JWT stocké en mémoire après la connexion
    // En production, on le lit depuis DataStore (stockage persistant)
    var accessToken: String? = null

    // Intercepteur qui ajoute le token JWT à chaque requête
    // C'est comme un "middleware" mais côté Android
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // Si on a un token, on l'ajoute dans le header Authorization
        val requestAvecToken = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        chain.proceed(requestAvecToken)
    }

    // Intercepteur de log (affiche les requêtes/réponses dans le terminal Android)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY  // Affiche tout en debug
        } else {
            HttpLoggingInterceptor.Level.NONE  // Rien en production
        }
    }

    // Client HTTP avec les intercepteurs configurés
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)     // Token JWT
        .addInterceptor(loggingInterceptor)  // Logs
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Instance Retrofit unique (singleton)
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)     // URL du backend depuis build.gradle
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())  // JSON → Kotlin
            .build()
            .create(ApiService::class.java)
    }
}
