package com.example.purrytify.network

import com.example.purrytify.model.LoginRequest
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.model.UserProfile
import com.example.purrytify.model.RefreshTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/profile")
    suspend fun getUserProfile(
    ): Response<UserProfile>

    @GET("api/verify-token")
    suspend fun verifyToken(
        @Header("Authorization") authHeader: String
    ): Response<Unit>

    @POST("api/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<LoginResponse>
}