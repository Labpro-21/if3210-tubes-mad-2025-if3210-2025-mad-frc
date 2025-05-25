package com.example.purrytify.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SoundCapsule(
    val month: YearMonth? = null,
    val timeListenedMillis: Long? = null,
    val topArtist: String? = null,
    val topSong: String? = null,
    val topArtistImageUrl: String? = null,
    val topSongImageUrl: String? = null,


    val longestDayStreak: Int? = 0,
    val streakSongTitle: String? = null,
    val streakSongArtist: String? = null,
    val streakSongArtworkPath: String? = null,
    val streakStartDate: LocalDate? = null,
    val streakEndDate: LocalDate? = null






) {
    /** "April 2025" */
    val monthYear: String
        get() = month
            ?.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        ?: ""

    fun formattedTimeListened(): String = timeListenedMillis?.let {
        val tot = it / 1000
        val hours = tot / 3600
        val minutes = (tot % 3600) / 60
        val seconds = tot % 60
        if (hours > 0) {
            String.format(Locale.getDefault(), "%d jam %02d menit", hours, minutes)
        } else {
            String.format(Locale.getDefault(), "%d menit %02d detik", minutes, seconds)
        }
    } ?: "0 menit 0 detik"

    val streakDescriptionText: String
        get() = if (longestDayStreak != null && longestDayStreak > 1 && streakSongTitle != null && streakSongArtist != null) {
            "Kamu memainkan $streakSongTitle oleh $streakSongArtist hari demi hari. Kamu membara!"
        } else if (longestDayStreak == 1 && streakSongTitle != null && streakSongArtist != null) {
            "Lagu streakmu adalah $streakSongTitle oleh $streakSongArtist."
        }
        else {
            "Belum ada streak yang tercatat bulan ini."
        }

    val streakPeriodText: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            return if (streakStartDate != null && streakEndDate != null) {
                if (streakStartDate == streakEndDate && longestDayStreak == 1) {
                    "Pada ${streakStartDate.format(formatter)}"
                } else if (longestDayStreak != null && longestDayStreak > 0) {
                    "Dari ${streakStartDate.format(formatter)} hingga ${streakEndDate.format(formatter)}"
                } else {
                    ""
                }
            } else {
                ""
            }
        }
}