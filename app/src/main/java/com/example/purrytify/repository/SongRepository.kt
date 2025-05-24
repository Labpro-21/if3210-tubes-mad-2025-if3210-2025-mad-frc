package com.example.purrytify.repository

import com.example.purrytify.data.SongDao
import com.example.purrytify.data.UserDao
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.Flow
import java.util.Date

class SongRepository(private val songDao: SongDao, private val userDao: UserDao) {
    suspend fun getSong(songId: Int): Song? = songDao.getSongById(songId)
    
    suspend fun insertSong(song: Song): Long {
        userDao.incrementSongs(song.userId)
        return songDao.insertSong(song)
    }
    
    suspend fun getAllExplicitlyAddedSongs(userId: Int): Flow<List<Song>> = songDao.getAllExplicitlyAddedSongs(userId)
    suspend fun getAllSongsInternal(userId: Int): Flow<List<Song>> = songDao.getAllSongsInternal(userId)
    
    suspend fun deleteSong(id: Int) {
        val song = getSong(id)
        if (song != null) {
            userDao.decrementSongs(song.userId)
            songDao.deleteSong(id)
        }
    }

    suspend fun getAllLikedSongs(userId: Int): Flow<List<Song>> = songDao.getAllLikedSongs(userId)
    suspend fun getAllSongsOrdered(userId: Int): Flow<List<Song>> = songDao.getAllSongsOrdered(userId)
    
    suspend fun updateSong(id:Int, newArtist: String, newTitle: String, newArtwork: String?, isExplicitlyAdded: Boolean) = songDao.updateSong(id, newArtist, newTitle, newArtwork, isExplicitlyAdded)
    
    suspend fun toggleLike(id:Int) {
        songDao.toggleLike(id)
        val song = getSong(id)
        if (song != null) {
            val updatedSong = songDao.getSongById(id) 
            if (updatedSong?.liked == true) {
                userDao.incrementLikedSongs(song.userId)
            } else {
                userDao.decrementLikedSongs(song.userId)
            }
        }
    }

    suspend fun getNewSongs(userId: Int): Flow<List<Song>> = songDao.getNewSongs(userId)
    suspend fun getRecentlyPlayed(userId: Int): Flow<List<Song>> = songDao.getRecentlyPlayed(userId)
    suspend fun incrementListenedSongs(userId: Int) = userDao.incrementListenedSongs(userId)
    suspend fun updateLastPlayed(songId:Int, lastPlayed:Date) = songDao.updateLastPlayed(songId,lastPlayed)
    suspend fun addPlayHistory(history: PlayHistory) {songDao.insertPlayHistory(history)}
    suspend fun getSongByAudioPathAndUserId(audioPath: String, userId: Int): Song? = songDao.getSongByAudioPathAndUserId(audioPath, userId)
}