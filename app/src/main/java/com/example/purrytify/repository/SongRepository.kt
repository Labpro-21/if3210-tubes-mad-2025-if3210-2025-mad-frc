package com.example.purrytify.repository

import com.example.purrytify.data.SongDao
import com.example.purrytify.data.UserDao
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow
import java.util.Date

class SongRepository(private val songDao: SongDao, private val userDao: UserDao) {
    suspend fun getSong(songId: Int): Song? = songDao.getSongById(songId)
    
    suspend fun insertSong(song: Song) {
        userDao.incrementSongs(song.userId)
        songDao.insertSong(song)
    }
    
    suspend fun getAllSongs(userId: Int): Flow<List<Song>> = songDao.getAllSongs(userId)
    
    suspend fun deleteSong(id: Int) {
        // Fix: Tambahkan null-safety check
        val song = getSong(id)
        if (song != null) {
            userDao.decrementSongs(song.userId)
            songDao.deleteSong(id)
        }
    }

    suspend fun getAllLikedSongs(userId: Int): Flow<List<Song>> = songDao.getAllLikedSongs(userId)
    suspend fun getAllSongsOrdered(userId: Int): Flow<List<Song>> = songDao.getAllSongsOrdered(userId)
    suspend fun updateSong(id:Int, newArtist: String, newTitle: String, newArtwork: String?) = songDao.updateSong(id,newArtist,newTitle,newArtwork)
    
    suspend fun toggleLike(id:Int) {
        songDao.toggleLike(id)
        // Fix: Tambahkan null-safety check
        val song = getSong(id)
        if (song != null) {
            if (song.liked) {
                userDao.incrementLikedSongs(song.userId)
            } else {
                userDao.decrementLikedSongs(song.userId)
            }
        }
    }

    suspend fun getNewSongs(userId: Int): Flow<List<Song>> = songDao.getNewSongs(userId)
    suspend fun getRecentlyPlayed(userId: Int): Flow<List<Song>> = songDao.getRecentlyPlayed(userId)
    suspend fun incrementListenedSongs(userId: Int) = userDao.incrementListenedSongs(userId)
    suspend fun updateLastPlayed(userId:Int, lastPlayed:Date) = songDao.updateLastPlayed(userId,lastPlayed)
}