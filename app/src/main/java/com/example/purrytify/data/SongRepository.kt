package com.example.purrytify.data

import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    suspend fun getSong(songId: Int) = songDao.getSongById(songId)
    suspend fun insertSong(song: Song) = songDao.insertSong(song)
    suspend fun getAllSongs(): List<Song> = songDao.getAllSongs()
    suspend fun deleteSong(id: Int) = songDao.deleteSong(id)
    suspend fun getAllLikedSongs():Flow<List<Song>> = songDao.getAllLikedSongs()
    suspend fun getAllSongsOrdered(): Flow<List<Song>> = songDao.getAllSongsOrdered()
    suspend fun updateSong(id:Int, newArtist: String, newTitle: String, newArtwork: String) = songDao.updateSong(id,newArtist,newTitle,newArtwork)
    suspend fun toggleLike(id:Int) = songDao.toggleLike(id)
}
