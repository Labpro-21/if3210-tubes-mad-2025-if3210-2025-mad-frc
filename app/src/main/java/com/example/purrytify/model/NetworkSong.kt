package com.example.purrytify.model

data class NetworkSong(
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,    // format "mm:ss"
    val country: String,
    val rank: Int
)