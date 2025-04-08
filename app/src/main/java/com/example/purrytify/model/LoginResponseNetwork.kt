package com.example.purrytify.model

import com.google.gson.annotations.SerializedName

data class LoginResponseNetwork(
    @SerializedName("token")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Int,

    @SerializedName("user")
    val userProfile: UserProfile?
) {
    fun getFullToken(): String = "$tokenType $accessToken"
}