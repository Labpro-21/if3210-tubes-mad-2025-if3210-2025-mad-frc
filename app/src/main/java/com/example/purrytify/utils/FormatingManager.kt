package com.example.purrytify.utils

object FormatingManager {
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds/1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}