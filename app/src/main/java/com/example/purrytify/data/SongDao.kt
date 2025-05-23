package com.example.purrytify.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SongDao {
    @Insert
    suspend fun insertSong(song: Song)

    @Query("SELECT * FROM Song WHERE user_id = :userId")
    fun getAllSongs(userId:Int): Flow<List<Song>>

    @Query("DELETE FROM Song WHERE id = :songId")
    suspend fun deleteSong(songId: Int)

    @Query("SELECT * FROM Song WHERE liked = 1 AND user_id = :userId ORDER BY lastPlayed")
    fun getAllLikedSongs(userId:Int): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE user_id = :userId ORDER BY lastPlayed")
    fun getAllSongsOrdered(userId:Int): Flow<List<Song>>

    @Query("UPDATE Song SET artist = :newArtist, title = :newTitle, artworkPath = :newArtwork WHERE id = :songId")
    suspend fun updateSong(songId: Int, newArtist: String, newTitle: String, newArtwork: String?)

    @Query("SELECT * FROM Song WHERE id = :songId")
    suspend fun getSongById(songId: Int): Song

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

    // Ubah topSong:
    @Query("SELECT s.title FROM play_history p JOIN song s ON p.song_id = s.id WHERE p.user_id = :userId AND strftime('%Y-%m', p.played_at/1000, 'unixepoch') = :yearMonth GROUP BY s.title ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun topSong(userId: Int, yearMonth: String): String?

    @Query("SELECT MAX(streak) FROM (SELECT COUNT(DISTINCT strftime('%Y-%m-%d', played_at/1000,'unixepoch')) AS streak FROM play_history WHERE user_id = :userId AND played_at >= :startOfMonth AND played_at < :startOfNextMonth GROUP BY strftime('%Y-%m-%d', played_at/1000,'unixepoch'))")
    suspend fun dayStreak(userId: Int, startOfMonth: Long, startOfNextMonth: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(history: PlayHistory)
}