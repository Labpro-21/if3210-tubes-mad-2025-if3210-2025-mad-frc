package com.example.purrytify.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow
import java.util.Date

// Data class untuk menampung hasil query streak
data class SongPlayDate(
    @ColumnInfo(name = "songId") val songId: Int,
    @ColumnInfo(name = "songTitle") val songTitle: String?,
    @ColumnInfo(name = "songArtist") val songArtist: String?,
    @ColumnInfo(name = "songArtworkPath") val songArtworkPath: String?,
    @ColumnInfo(name = "playDate") val playDate: String // Format "YYYY-MM-DD"
)

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: Song): Long

    @Query("SELECT * FROM Song WHERE user_id = :userId AND isExplicitlyAdded = 1 ORDER BY title ASC")
    fun getAllExplicitlyAddedSongs(userId:Int): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE user_id = :userId ORDER BY title ASC")
    fun getAllSongsInternal(userId:Int): Flow<List<Song>>

    @Query("DELETE FROM Song WHERE id = :songId")
    suspend fun deleteSong(songId: Int)

    @Query("SELECT * FROM Song WHERE liked = 1 AND user_id = :userId ORDER BY lastPlayed DESC")
    fun getAllLikedSongs(userId:Int): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE user_id = :userId ORDER BY lastPlayed DESC")
    fun getAllSongsOrdered(userId:Int): Flow<List<Song>>

    @Query("UPDATE Song SET artist = :newArtist, title = :newTitle, artworkPath = :newArtwork, isExplicitlyAdded = :isExplicitlyAdded WHERE id = :songId")
    suspend fun updateSong(songId: Int, newArtist: String, newTitle: String, newArtwork: String?, isExplicitlyAdded: Boolean)

    @Query("SELECT * FROM Song WHERE id = :songId")
    suspend fun getSongById(songId: Int): Song?

    @Query("UPDATE Song SET liked = NOT liked WHERE id = :songId")
    suspend fun toggleLike(songId: Int)

    @Query("SELECT * FROM Song WHERE user_id = :userId ORDER BY addedDate DESC LIMIT 5")
    fun getNewSongs(userId:Int): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE lastPlayed IS NOT NULL AND user_id = :userId ORDER BY lastPlayed DESC LIMIT 5")
    fun getRecentlyPlayed(userId:Int): Flow<List<Song>>

    @Query("UPDATE Song SET lastPlayed = :lastPlayed WHERE id = :songId")
    suspend fun updateLastPlayed(songId: Int,lastPlayed: Date)

    @Query("SELECT SUM(duration_ms) FROM play_history WHERE user_id = :userId AND strftime('%Y-%m', played_at/1000, 'unixepoch') = :yearMonth")
    suspend fun sumTimeListened(userId: Int, yearMonth: String): Long?

    @Query("SELECT s.artist FROM play_history p JOIN song s ON p.song_id = s.id WHERE p.user_id = :userId AND strftime('%Y-%m', p.played_at/1000, 'unixepoch') = :yearMonth GROUP BY s.artist ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun topArtist(userId: Int, yearMonth: String): String?

    @Query("SELECT s.title FROM play_history p JOIN song s ON p.song_id = s.id WHERE p.user_id = :userId AND strftime('%Y-%m', p.played_at/1000, 'unixepoch') = :yearMonth GROUP BY s.title ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun topSong(userId: Int, yearMonth: String): String?

    // Query baru untuk mendapatkan data mentah perhitungan streak
    @Query("""
        SELECT 
            ph.song_id as songId, 
            s.title AS songTitle, 
            s.artist AS songArtist, 
            s.artworkPath AS songArtworkPath, 
            strftime('%Y-%m-%d', ph.played_at/1000, 'unixepoch') AS playDate
        FROM play_history ph
        JOIN song s ON ph.song_id = s.id
        WHERE ph.user_id = :userId 
          AND ph.played_at >= :startOfMonthMillis 
          AND ph.played_at < :startOfNextMonthMillis
        ORDER BY ph.song_id, playDate ASC
    """)
    suspend fun getPlayHistoryDatesForStreak(userId: Int, startOfMonthMillis: Long, startOfNextMonthMillis: Long): List<SongPlayDate>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(history: PlayHistory)

    @Query("SELECT * FROM Song WHERE audioPath = :audioPath AND user_id = :userId LIMIT 1")
    suspend fun getSongByAudioPathAndUserId(audioPath: String, userId: Int): Song?
}