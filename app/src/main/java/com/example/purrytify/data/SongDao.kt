package com.example.purrytify.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
    suspend fun getSongById(songId: Int): Song?

    @Query("UPDATE Song SET liked = NOT liked WHERE id = :songId")
    suspend fun toggleLike(songId: Int)

    @Query("SELECT * FROM Song WHERE user_id = :userId ORDER BY addedDate DESC LIMIT 5")
    fun getNewSongs(userId:Int): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE lastPlayed IS NOT NULL AND user_id = :userId ORDER BY lastPlayed DESC LIMIT 5")
    fun getRecentlyPlayed(userId:Int): Flow<List<Song>>

    @Query("UPDATE Song SET lastPlayed = :lastPlayed WHERE id = :songId")
    suspend fun updateLastPlayed(songId: Int,lastPlayed: Date)
}