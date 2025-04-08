package com.example.purrytify.model

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showDialog: Boolean = false
)