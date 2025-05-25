package com.example.purrytify.repository

import com.example.purrytify.data.SongDao
import com.example.purrytify.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationRepository(private val songDao: SongDao) {

    suspend fun getRandomExplicitlyAddedSongs(userId: Int, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        songDao.getRandomExplicitlyAddedSongs(userId, limit)
    }

    suspend fun getRandomLikedSongs(userId: Int, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        songDao.getRandomLikedSongs(userId, limit)
    }

    suspend fun doesSongExistByAudioPath(userId: Int, audioPath: String): Boolean = withContext(Dispatchers.IO) {
        songDao.existsByAudioPath(userId, audioPath)
    }
}