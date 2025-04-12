package com.example.purrytify.repository

import com.example.purrytify.data.UserDao
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

    suspend fun incrementSongs(userId: Int) = userDao.incrementSongs(userId)
    suspend fun incrementListenedSongs(userId: Int) = userDao.incrementListenedSongs(userId)
    suspend fun incrementLikedSongs(userId: Int) = userDao.incrementLikedSongs(userId)

    suspend fun decrementLikedSongs(userId: Int) = userDao.decrementLikedSongs(userId)
    suspend fun decrementSongs(userId: Int) = userDao.decrementSongs(userId)
    suspend fun decrementListenedSongs(userId: Int) = userDao.decrementListenedSongs(userId)
}