package com.example.purrytify.model

data class ProfileUiState(
    val username: String = "",
    val email: String = "",
    val profilePhoto: Int? = null, // resource drawable; gunakan null jika belum ada
    val songsAdded: Int = 0,
    val likedSongs: Int = 0,
    val listenedSongs: Int = 0
)