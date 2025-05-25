package com.example.purrytify.repository

import android.content.Context
import android.util.Log
import com.example.purrytify.data.ArtistRankInfo
import com.example.purrytify.data.SongDao
import com.example.purrytify.data.SongPlayDate // Pastikan ini diimpor
import com.example.purrytify.model.Song
import com.example.purrytify.model.SoundCapsule
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class AnalyticsRepository(
    private val dao: SongDao,
    private val context: Context
) {
    suspend fun getMonthlyAnalytics(userId: Int, yearMonthString: String): SoundCapsule {
        val month = YearMonth.parse(yearMonthString) // yearMonthString harus "YYYY-MM"
        val startOfMonth = month.atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val startOfNextMonth = month.plusMonths(1)
            .atDay(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val timeListened = dao.sumTimeListened(userId, yearMonthString)
        val topArtistName = dao.topArtist(userId, yearMonthString)
        val topSongTitle = dao.topSong(userId, yearMonthString)

        // Perhitungan Streak Baru
        val playHistoryForStreak = dao.getPlayHistoryDatesForStreak(userId, startOfMonth, startOfNextMonth)
        Log.d("AnalyticsRepository", "Play history for streak calculation (user $userId, month $yearMonthString): ${playHistoryForStreak.size} entries")


        var overallLongestStreak = 0
        var streakSongDetails: SongPlayDate? = null
        var finalStreakStartDate: LocalDate? = null
        var finalStreakEndDate: LocalDate? = null

        if (playHistoryForStreak.isNotEmpty()) {
            val playsBySongId = playHistoryForStreak.groupBy { it.songId }

            playsBySongId.forEach { (_, playsForOneSong) ->
                if (playsForOneSong.isEmpty()) return@forEach

                val uniqueSortedDates = playsForOneSong
                    .mapNotNull {
                        try {
                            LocalDate.parse(it.playDate, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: Exception) {
                            Log.e("AnalyticsRepository", "Error parsing date: ${it.playDate}", e)
                            null
                        }
                    }
                    .distinct()
                    .sorted()

                if (uniqueSortedDates.isEmpty()) return@forEach

                var currentStreak = 0
                var longestStreakForThisSong = 0
                var currentStreakStartDate: LocalDate? = null
                var tempStreakEndDate: LocalDate? = null

                var songSpecificLongestStreakStartDate: LocalDate? = null
                var songSpecificLongestStreakEndDate: LocalDate? = null


                for (i in uniqueSortedDates.indices) {
                    val currentDate = uniqueSortedDates[i]
                    if (i == 0 || ChronoUnit.DAYS.between(uniqueSortedDates[i-1], currentDate) != 1L) {
                        // Streak putus atau awal streak baru
                        if (currentStreak > longestStreakForThisSong) {
                            longestStreakForThisSong = currentStreak
                            songSpecificLongestStreakStartDate = currentStreakStartDate
                            songSpecificLongestStreakEndDate = tempStreakEndDate
                        }
                        currentStreak = 1
                        currentStreakStartDate = currentDate
                    } else {
                        // Streak berlanjut
                        currentStreak++
                    }
                    tempStreakEndDate = currentDate
                }

                // Cek terakhir setelah loop untuk streak terakhir
                if (currentStreak > longestStreakForThisSong) {
                    longestStreakForThisSong = currentStreak
                    songSpecificLongestStreakStartDate = currentStreakStartDate
                    songSpecificLongestStreakEndDate = tempStreakEndDate
                }
                
                Log.d("AnalyticsRepository", "Song ID ${playsForOneSong.first().songId}: Longest streak for this song = $longestStreakForThisSong days")

                if (longestStreakForThisSong > overallLongestStreak) {
                    overallLongestStreak = longestStreakForThisSong
                    streakSongDetails = playsForOneSong.first() // Ambil detail dari salah satu play
                    finalStreakStartDate = songSpecificLongestStreakStartDate
                    finalStreakEndDate = songSpecificLongestStreakEndDate
                } else if (longestStreakForThisSong == overallLongestStreak && overallLongestStreak > 0) {
                    // Jika ada streak yang sama panjang, bisa pilih berdasarkan kriteria lain (misal lagu paling sering diputar dalam streak itu, atau yang terbaru)
                    // Untuk sekarang, kita ambil yang pertama ditemukan dengan panjang sama.
                }
            }
        }
        Log.d("AnalyticsRepository", "Overall longest streak: $overallLongestStreak days for song: ${streakSongDetails?.songTitle}")


        // Dapatkan URL artwork untuk topArtist dan topSong (ini perlu logika tambahan jika ingin gambar spesifik)
        // Untuk sekarang, kita biarkan null atau Anda bisa coba ambil dari lagu pertama yang terkait.
        // Ini adalah placeholder, Anda mungkin perlu query tambahan untuk artwork top artist/song.
        val topArtistArt = if (topArtistName != null) dao.getPlayHistoryDatesForStreak(userId, startOfMonth, startOfNextMonth).find { it.songArtist == topArtistName }?.songArtworkPath else null
        val topSongArt = if (topSongTitle != null) dao.getPlayHistoryDatesForStreak(userId, startOfMonth, startOfNextMonth).find { it.songTitle == topSongTitle }?.songArtworkPath else null


        return SoundCapsule(
            month = month,
            timeListenedMillis = timeListened,
            topArtist = topArtistName,
            topSong = topSongTitle,
            topArtistImageUrl = topArtistArt, // Placeholder
            topSongImageUrl = topSongArt,   // Placeholder

            longestDayStreak = if (overallLongestStreak > 0) overallLongestStreak else null, // Hanya set jika streak > 0
            streakSongTitle = streakSongDetails?.songTitle,
            streakSongArtist = streakSongDetails?.songArtist,
            streakSongArtworkPath = streakSongDetails?.songArtworkPath,
            streakStartDate = finalStreakStartDate,
            streakEndDate = finalStreakEndDate
        )
    }

    // Fungsi exportToCsv tetap sama, tapi akan menggunakan field baru dari SoundCapsule
    fun exportToCsv(
        soundCapsule: SoundCapsule,
        topArtists: List<ArtistRankInfo>,
        topSongs: List<Song>
    ): File? {
        if (soundCapsule.month == null) {
            Log.w("AnalyticsRepository", "Cannot export CSV, SoundCapsule month is null.")
            return null
        }

        val monthYearStr = soundCapsule.month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val fileName = "Purrytify_Analytics_${monthYearStr}_${System.currentTimeMillis()}.csv"

        val csvHeader = "Category,Item,Details\n"
        val csvContent = buildString {
            append(csvHeader)

            // General Info
            appendLine("Summary,Month,\"${soundCapsule.monthYear}\"")
            appendLine("Summary,Time Listened,\"${soundCapsule.formattedTimeListened()}\"")
            appendLine("Summary,Top Artist Overall,\"${soundCapsule.topArtist ?: "-"}\"")
            appendLine("Summary,Top Song Overall,\"${soundCapsule.topSong ?: "-"}\"")
            appendLine("Summary,Longest Day Streak,\"${soundCapsule.longestDayStreak?.toString() ?: "0"} days\"")
            if (soundCapsule.longestDayStreak != null && soundCapsule.longestDayStreak > 0) {
                appendLine("Summary,Streak Song Title,\"${soundCapsule.streakSongTitle ?: "-"}\"")
                appendLine("Summary,Streak Song Artist,\"${soundCapsule.streakSongArtist ?: "-"}\"")
                appendLine("Summary,Streak Period,\"${soundCapsule.streakPeriodText.replace(",", ";")}\"")
            }
            appendLine() // Baris kosong sebagai pemisah

            // Top Artists List
            appendLine("Top Artists for ${soundCapsule.monthYear}")
            appendLine("Rank,Artist Name")
            topArtists.forEachIndexed { index, artistInfo ->
                appendLine("${index + 1},\"${artistInfo.artistName.replace("\"", "\"\"")}\"")
            }
            appendLine()

            // Top Songs List
            appendLine("Top Songs for ${soundCapsule.monthYear}")
            appendLine("Rank,Song Title,Artist")
            topSongs.forEachIndexed { index, song ->
                appendLine("${index + 1},\"${song.title.replace("\"", "\"\"")}\",\"${song.artist.replace("\"", "\"\"")}\"")
            }
        }

        return try {
            // Menggunakan direktori cache internal aplikasi
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            file.writeText(csvContent)
            Log.i("AnalyticsRepository", "CSV exported successfully to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("AnalyticsRepository", "Error exporting CSV: ${e.message}", e)
            null
        }
    }

    fun getTopPlayedSongsForMonth(userId: Int, yearMonth: String): Flow<List<Song>> {
        Log.d("AnalyticsRepository", "Fetching top played songs for user $userId, month $yearMonth")
        return dao.getTopPlayedSongsForMonth(userId, yearMonth)
    }

    fun getTopPlayedArtistsForMonth(userId: Int, yearMonth: String): Flow<List<ArtistRankInfo>> {
        Log.d("AnalyticsRepository", "Fetching top played artists for user $userId, month $yearMonth")
        return dao.getTopPlayedArtistsForMonth(userId, yearMonth)
    }

    suspend fun getTotalDistinctArtistsForMonth(userId: Int, yearMonth: String): Int {
        return dao.getTotalDistinctArtistsForMonth(userId, yearMonth)
    }
}