package com.example.purrytify.repository

import android.content.Context
import com.example.purrytify.data.SongDao
import com.example.purrytify.model.SoundCapsule
import java.io.File
import java.time.YearMonth
import java.time.ZoneId

class AnalyticsRepository(
    private val dao: SongDao,
    private val context: Context          // ← tambahkan Context
) {
    suspend fun getMonthlyAnalytics(userId: Int, month: YearMonth): SoundCapsule {
        val ym = month.toString() // “YYYY-MM”
        val start = month.atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val next = month.plusMonths(1)
            .atDay(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        return SoundCapsule(
            month = month,
            timeListenedMillis = dao.sumTimeListened(userId, ym),
            topArtist = dao.topArtist(userId, ym),
            topSong = dao.topSong(userId, ym),
            dayStreak = dao.dayStreak(userId, start, next)
        )
    }

    fun exportToCsv(soundCapsule: SoundCapsule): File {
        val csv = buildString {
            appendLine("Month,Time listened,Top artist,Top song,Day streak")
            appendLine(
                listOf(
                    soundCapsule.month,
                    soundCapsule.formattedTimeListened(),
                    soundCapsule.topArtist ?: "",
                    soundCapsule.topSong ?: "",
                    soundCapsule.dayStreak?.toString() ?: ""
                ).joinToString(separator = ",")
            )
        }
        // ganti 'app.filesDir' dengan context.filesDir
        val file = File(context.filesDir, "analytics_${soundCapsule.month}.csv")
        file.writeText(csv)
        return file
    }
}