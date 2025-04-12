package com.example.purrytify.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName= "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val email: String,
    val songs: Int,
    val likedSongs: Int,
    val listenedSongs: Int,
)