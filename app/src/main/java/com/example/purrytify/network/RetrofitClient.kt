package com.example.purrytify.network

import com.example.purrytify.utils.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor


object RetrofitClient {
    private const val BASE_URL = "http://34.101.226.132:3000/"

    // Instance Retrofit khusus untuk login (tanpa authenticator)
    private val loginRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Properti ini digunakan untuk endpoint login yang tidak memerlukan token.
    val loginApiService: ApiService by lazy {
        loginRetrofit.create(ApiService::class.java)
    }

    // Instance Retrofit untuk endpoint yang memerlukan token.
    fun create(tokenManager: TokenManager): ApiService {

        val client = OkHttpClient.Builder()
            .authenticator(TokenAuthenticator(tokenManager))
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val accessToken = tokenManager.getAccessToken()
                val newRequest = if (!accessToken.isNullOrEmpty()) {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                } else {
                    originalRequest
                }
                chain.proceed(newRequest)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Fungsi baru untuk keperluan verifikasi token (tanpa authenticator atau TokenManager)
    fun verifyApiService(token: String): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}