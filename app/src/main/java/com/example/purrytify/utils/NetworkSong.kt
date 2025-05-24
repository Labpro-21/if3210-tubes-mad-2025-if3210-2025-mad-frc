package com.example.purrytify.utils

import com.example.purrytify.model.NetworkSong     // fijasi: model.NetworkSong data class
import com.example.purrytify.model.Song
import java.util.Date

fun NetworkSong.toLocalSong(userId: Int): Song {
    val ms = try {
        val parts = this.duration.split(":")
        if (parts.size == 2) {
            ((parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)) * 1000
        } else 0L
    } catch (e: Exception) {
        0L
    }
    return Song(
        id = 0,
        serverId = this.id,
        title = this.title,
        artist = this.artist,
        duration = ms,
        artworkPath = this.artwork,
        audioPath = this.url,
        addedDate = Date(),
        lastPlayed = null,
        userId = userId,
        isExplicitlyAdded = false
    )
}
