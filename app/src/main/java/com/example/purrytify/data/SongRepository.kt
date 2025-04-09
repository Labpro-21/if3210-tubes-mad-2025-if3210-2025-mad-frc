package com.example.purrytify.data

import com.example.purrytify.model.Song

class SongRepository(private val songDao: SongDao) {
    suspend fun insertSong(song: Song) = songDao.insertSong(song)
    suspend fun getAllSongs(): List<Song> = songDao.getAllSongs()
    suspend fun deleteSong(id: String) = songDao.deleteSong(id)
}
