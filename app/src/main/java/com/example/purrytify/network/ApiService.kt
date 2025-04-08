package com.example.purrytify.network

import com.example.purrytify.model.LoginRequest
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Mendapatkan data profile pengguna
    // Header "Authorization" harus berisi "Bearer {token}"
    @GET("api/profile")
    suspend fun getUserProfile(
    @Header("Authorization") authToken: String
    ): Response<UserProfile>
}