package com.example.purrytify.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,

    val expiryTimeMillis: Long = System.currentTimeMillis() + 5 * 60 * 1000
)