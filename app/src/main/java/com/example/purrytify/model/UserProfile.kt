package com.example.purrytify.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName= "user")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val username: String,
    val email: String,
    val profilePhoto: String, // URL foto profil
    val location: String,
    val createdAt: String,
    val updatedAt: String,
)
