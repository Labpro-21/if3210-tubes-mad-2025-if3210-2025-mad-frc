package com.example.purrytify.model

import java.time.YearMonth

/**
 * Representasi data analitik bulanan (“Sound Capsule”) untuk setiap pengguna.
 *
 * @property month          Bulan dan tahun (YearMonth) data ini diambil.
 * @property timeListenedMillis  Total durasi mendengarkan (ms) pada bulan tersebut.
 * @property topArtist      Artis yang paling sering didengar.
 * @property topSong        Lagu yang paling sering diputar.
 * @property dayStreak      Jumlah hari berturut-turut (>=2 hari) mendengarkan lagu.
 */
data class SoundCapsule(
    val month: YearMonth? = null,
    val timeListenedMillis: Long? = null,
    val topArtist: String? = null,
    val topSong: String? = null,
    val dayStreak: Int? = null
) {
    /**
     * Format durasi listen dalam "H:mm:ss", atau teks fallback jika null.
     */
    fun formattedTimeListened(): String = timeListenedMillis?.let {
        val totalSec = it / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        "%d:%02d:%02d".format(h, m, s)
    } ?: "No data available"
}