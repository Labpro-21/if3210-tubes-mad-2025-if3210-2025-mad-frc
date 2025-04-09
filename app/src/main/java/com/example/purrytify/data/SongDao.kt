package com.example.purrytify.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert
    suspend fun insertSong(song: Song)

    @Query("SELECT * FROM Song")
    suspend fun getAllSongs(): List<Song>

    @Query("DELETE FROM Song WHERE id = :songId")
    suspend fun deleteSong(songId: Int)

    @Query("SELECT * FROM Song WHERE liked = 1 ORDER BY lastPlayed")
    fun getAllLikedSongs(): Flow<List<Song>>


    @Query("SELECT * FROM Song ORDER BY lastPlayed")
    fun getAllSongsOrdered(): Flow<List<Song>>

    @Query("UPDATE Song SET artist = :newArtist, title = :newTitle, artworkPath = :newArtwork WHERE id = :songId")
    suspend fun updateSong(songId: Int, newArtist: String, newTitle: String, newArtwork: String)

    @Query("SELECT * FROM Song WHERE id = :songId")
    fun getSongById(songId: Int): Song

}