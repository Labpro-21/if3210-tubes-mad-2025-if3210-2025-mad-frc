package com.example.purrytify.data

import com.example.purrytify.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun getUserById(id: Int): User? = userDao.getUserById(id)

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)

    suspend fun isEmailRegistered(email: String) = userDao.isEmailRegistered(email)

    suspend fun getUserIdByEmail(email: String) = userDao.getUserIdByEmail(email)

    suspend fun getLikedSongs(userId: Int) = userDao.getLikedSongs(userId)
    suspend fun getSongs(userId: Int) = userDao.getSongs(userId)
    suspend fun getListenedSongs(userId: Int) = userDao.getListenedSongs(userId)

    suspend fun updateLikedSongs(userId: Int, likedSongs: Int) = userDao.updateLikedSongs(userId, likedSongs)
    suspend fun updateSongs(userId: Int, songs: Int) = userDao.updateSongs(userId, songs)
    suspend fun updateListenedSongs(userId: Int, listenedSongs: Int) = userDao.updateListenedSongs(userId, listenedSongs)

}