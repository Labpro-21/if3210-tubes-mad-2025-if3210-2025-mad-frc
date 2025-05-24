package com.example.purrytify.network

import com.example.purrytify.model.LoginRequest
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.model.UserProfile
import com.example.purrytify.model.RefreshTokenRequest
import com.example.purrytify.model.NetworkSong
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

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

    @GET("api/top-songs/global")
    suspend fun getGlobalTopSongs(): Response<List<NetworkSong>>

    @GET("api/top-songs/{country_code}")
    suspend fun getTopSongsByCountry(@Path("country_code") code: String): Response<List<NetworkSong>>

    @GET("api/songs/{song_id}")
    suspend fun getSongDetail(@Path("song_id") id: Int): Response<NetworkSong>
}