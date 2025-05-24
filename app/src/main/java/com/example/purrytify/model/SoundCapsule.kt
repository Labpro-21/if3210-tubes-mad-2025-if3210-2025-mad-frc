package com.example.purrytify.model

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SoundCapsule(
    val month: YearMonth? = null,
    val timeListenedMillis: Long? = null,
    val topArtist: String? = null,
    val topSong: String? = null,
    val dayStreak: Int? = null,

    val topArtistImageUrl: String?   = null,
    val topSongImageUrl: String?     = null,
    val streakCoverUrl: String?      = null,
    val streakDescription: String?   = null,
    val streakPeriod: String? = null
) {
    /** "April 2025" */
    val monthYear: String
        get() = month
            ?.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            ?.replaceFirstChar { it.uppercase() }
        ?: ""

    fun formattedTimeListened(): String = timeListenedMillis?.let {
        val tot = it/1000
        "%d:%02d:%02d".format(tot/3600, (tot%3600)/60, tot%60)
    } ?: "No data available"
}