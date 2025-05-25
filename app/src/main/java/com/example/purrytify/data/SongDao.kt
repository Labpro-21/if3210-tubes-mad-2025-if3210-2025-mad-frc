package com.example.purrytify.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow
import java.util.Date


data class SongPlayDate(
    @ColumnInfo(name = "songId") val songId: Int,
    @ColumnInfo(name = "songTitle") val songTitle: String?,
    @ColumnInfo(name = "songArtist") val songArtist: String?,
    @ColumnInfo(name = "songArtworkPath") val songArtworkPath: String?,
    @ColumnInfo(name = "playDate") val playDate: String
)

data class DailyListenDuration(
    @ColumnInfo(name = "playDate") val playDate: String,
    @ColumnInfo(name = "totalDurationMillis") val totalDurationMillis: Long
)

data class ArtistRankInfo(
    @ColumnInfo(name = "artistName") val artistName: String,
    @ColumnInfo(name = "artworkPath") val artworkPath: String?,
    @ColumnInfo(name = "totalPlays") val totalPlays: Int
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

    @Query("SELECT * FROM Song WHERE user_id = :userId AND isExplicitlyAdded = 1 ORDER BY addedDate DESC LIMIT 5")
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

    @Query("SELECT ph.song_id as songId, s.title AS songTitle, s.artist AS songArtist, s.artworkPath AS songArtworkPath, strftime('%Y-%m-%d', ph.played_at/1000, 'unixepoch') AS playDate FROM play_history ph JOIN song s ON ph.song_id = s.id WHERE ph.user_id = :userId AND ph.played_at >= :startOfMonthMillis AND ph.played_at < :startOfNextMonthMillis ORDER BY ph.song_id, playDate ASC")
    suspend fun getPlayHistoryDatesForStreak(userId: Int, startOfMonthMillis: Long, startOfNextMonthMillis: Long): List<SongPlayDate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(history: PlayHistory)

    @Query("SELECT * FROM Song WHERE audioPath = :audioPath AND user_id = :userId LIMIT 1")
    suspend fun getSongByAudioPathAndUserId(audioPath: String, userId: Int): Song?

    @Query("SELECT strftime('%Y-%m-%d', ph.played_at/1000, 'unixepoch') AS playDate, SUM(ph.duration_ms) AS totalDurationMillis FROM play_history ph WHERE ph.user_id = :userId AND ph.played_at >= :startOfMonthMillis AND ph.played_at < :startOfNextMonthMillis GROUP BY playDate ORDER BY playDate ASC")
    suspend fun getDailyListenDurationsForMonth(userId: Int, startOfMonthMillis: Long, startOfNextMonthMillis: Long): List<DailyListenDuration>

    @Query("SELECT s.* FROM song s INNER JOIN ( SELECT song_id, COUNT(song_id) as play_count FROM play_history WHERE user_id = :userId AND strftime('%Y-%m', played_at/1000, 'unixepoch') = :yearMonth GROUP BY song_id) AS ph_counts ON s.id = ph_counts.song_id WHERE s.user_id = :userId ORDER BY ph_counts.play_count DESC LIMIT 10")
    fun getTopPlayedSongsForMonth(userId: Int, yearMonth: String): Flow<List<Song>>

    @Query("SELECT s.artist AS artistName,(SELECT s_inner.artworkPath FROM song s_inner INNER JOIN play_history ph_inner ON s_inner.id = ph_inner.song_id WHERE s_inner.artist = s.artist AND s_inner.user_id = :userId AND strftime('%Y-%m', ph_inner.played_at/1000, 'unixepoch') = :yearMonth ORDER BY ph_inner.played_at DESC LIMIT 1) AS artworkPath, COUNT(ph.song_id) as totalPlays FROM song s INNER JOIN play_history ph ON s.id = ph.song_id WHERE s.user_id = :userId AND strftime('%Y-%m', ph.played_at/1000, 'unixepoch') = :yearMonth AND s.artist IS NOT NULL AND s.artist != '' GROUP BY s.artist ORDER BY totalPlays DESC LIMIT 10")
    fun getTopPlayedArtistsForMonth(userId: Int, yearMonth: String): Flow<List<ArtistRankInfo>>

    @Query("SELECT COUNT(DISTINCT s.artist) FROM song s INNER JOIN play_history ph ON s.id = ph.song_id WHERE s.user_id = :userId AND strftime('%Y-%m', ph.played_at/1000, 'unixepoch') = :yearMonth AND s.artist IS NOT NULL AND s.artist != ''")
    suspend fun getTotalDistinctArtistsForMonth(userId: Int, yearMonth: String): Int

    @Query("SELECT * FROM song WHERE user_id = :userId AND isExplicitlyAdded = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomExplicitlyAddedSongs(userId: Int, limit: Int): List<Song>

    @Query("SELECT * FROM song WHERE user_id = :userId AND liked = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomLikedSongs(userId: Int, limit: Int): List<Song>

    @Query("SELECT EXISTS(SELECT 1 FROM song WHERE user_id = :userId AND audioPath = :audioPath LIMIT 1)")
    suspend fun existsByAudioPath(userId: Int, audioPath: String): Boolean

    @Query("""
    SELECT EXISTS(
      SELECT 1 FROM song
       WHERE server_id = :serverId
         AND isExplicitlyAdded = 1
    )
  """)
    suspend fun existsByServerIdExplicitlyAdded(serverId: Int?): Boolean
}