package com.example.purrytify.repository

import com.example.purrytify.data.SongDao
import com.example.purrytify.data.UserDao
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao, private val userDao: UserDao) {
    suspend fun getSong(songId: Int) = songDao.getSongById(songId)
    suspend fun insertSong(song: Song) {
        songDao.insertSong(song)
        userDao.incrementSongs(song.userId)
    }
    suspend fun getAllSongs(): List<Song> = songDao.getAllSongs()
    suspend fun deleteSong(id: Int){
        songDao.deleteSong(id)
        userDao.decrementSongs(getSong(id).userId)
    }

    suspend fun getAllLikedSongs(): Flow<List<Song>> = songDao.getAllLikedSongs()
    suspend fun getAllSongsOrdered(): Flow<List<Song>> = songDao.getAllSongsOrdered()
    suspend fun updateSong(id:Int, newArtist: String, newTitle: String, newArtwork: String?) = songDao.updateSong(id,newArtist,newTitle,newArtwork)
    suspend fun toggleLike(id:Int) {
        songDao.toggleLike(id)
        val song = getSong(id)
        if (song.liked) {
            userDao.incrementLikedSongs(song.userId)
        } else {
            userDao.decrementLikedSongs(song.userId)
        }
    }

    suspend fun getNewSongs(): Flow<List<Song>> = songDao.getNewSongs()
    suspend fun getRecentlyPlayed(): Flow<List<Song>> = songDao.getRecentlyPlayed()
    suspend fun incrementListenedSongs(userId: Int) = userDao.incrementListenedSongs(userId)
}