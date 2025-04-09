package com.example.purrytify.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.purrytify.model.Song

@Dao
interface SongDao {
    @Insert
    suspend fun insertSong(song: Song)

    @Query("SELECT * FROM Song")
    suspend fun getAllSongs(): List<Song>

    @Query("DELETE FROM Song WHERE id = :songId")
    suspend fun deleteSong(songId: String)

    @Query("SELECT * FROM Song ORDER BY id DESC LIMIT 5")
    suspend fun getNewSongs(): List<Song>


}