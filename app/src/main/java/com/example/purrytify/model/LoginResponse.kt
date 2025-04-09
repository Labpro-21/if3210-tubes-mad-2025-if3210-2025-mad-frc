package com.example.purrytify.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    // Expiry time dalam milidetik; akan dihitung di TokenManager (misalnya 5 menit)
    val expiryTimeMillis: Long = System.currentTimeMillis() + 5 * 60 * 1000
)