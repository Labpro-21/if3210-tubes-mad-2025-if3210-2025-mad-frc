package com.example.purrytify.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val artwork: String,
    val path: String
)
