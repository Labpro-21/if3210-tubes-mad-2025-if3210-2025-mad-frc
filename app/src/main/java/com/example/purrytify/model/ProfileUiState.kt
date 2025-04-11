package com.example.purrytify.model

data class ProfileUiState(
    val username: String = "",
    val email: String = "",
    val country: String = "",
    val profilePhoto: String = "", // ubah dari Int? menjadi String
    val songsAdded: Int = 0,
    val likedSongs: Int = 0,
    val listenedSongs: Int = 0
)