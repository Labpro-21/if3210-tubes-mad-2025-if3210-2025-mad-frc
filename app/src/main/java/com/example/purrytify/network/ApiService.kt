package com.example.purrytify.network

import com.example.purrytify.model.LoginResponseNetwork
import com.example.purrytify.model.Song
import com.example.purrytify.model.UserProfile
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ================= Authentication =================
    @POST("api/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<LoginResponseNetwork>

    @POST("api/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Void>

    // ================== User Profile ==================
    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>

    // =================== Song Management ===================
    @GET("api/songs")
    suspend fun getAllSongs(
        @Header("Authorization") token: String
    ): Response<List<Song>>

    @Multipart
    @POST("api/songs")
    suspend fun addSong(
        @Header("Authorization") token: String,
        @Part("title") title: RequestBody,
        @Part("artist") artist: RequestBody,
        @Part audioFile: MultipartBody.Part,
        @Part artwork: MultipartBody.Part?
    ): Response<Song>

    @GET("api/songs/recent")
    suspend fun getRecentSongs(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 10
    ): Response<List<Song>>

    // =================== Likes Management ===================
    @GET("api/songs/liked")
    suspend fun getLikedSongs(
        @Header("Authorization") token: String
    ): Response<List<Song>>

    @POST("api/songs/{songId}/like")
    suspend fun likeSong(
        @Header("Authorization") token: String,
        @Path("songId") songId: String
    ): Response<Void>

    @DELETE("api/songs/{songId}/like")
    suspend fun unlikeSong(
        @Header("Authorization") token: String,
        @Path("songId") songId: String
    ): Response<Void>

    // =================== Token Management ===================
    @POST("api/auth/refresh")
    @FormUrlEncoded
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String
    ): Response<LoginResponseNetwork>

    @GET("api/auth/verify")
    suspend fun verifyToken(
        @Header("Authorization") token: String
    ): Response<Void>
}