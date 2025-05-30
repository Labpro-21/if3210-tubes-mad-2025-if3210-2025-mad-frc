package com.example.purrytify.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.purrytify.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<User>>

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT id FROM user WHERE email = :email LIMIT 1")
    suspend fun getUserIdByEmail(email: String): Int?

    @Query("SELECT EXISTS(SELECT 1 FROM user WHERE email = :email)")
    suspend fun isEmailRegistered(email: String): Boolean


    @Query("SELECT likedSongs FROM user WHERE id = :userId")
    suspend fun getLikedSongs(userId: Int): Int?

    @Query("SELECT songs FROM user WHERE id = :userId")
    suspend fun getSongs(userId: Int): Int?

    @Query("SELECT listenedSongs FROM user WHERE id = :userId")
    suspend fun getListenedSongs(userId: Int): Int?

    @Query("UPDATE user SET songs = songs + 1 WHERE id = :userId")
    suspend fun incrementSongs(userId: Int)

    @Query("UPDATE user SET listenedSongs = listenedSongs + 1 WHERE id = :userId")
    suspend fun incrementListenedSongs(userId: Int)

    @Query("UPDATE user SET likedSongs = likedSongs + 1 WHERE id = :userId")
    suspend fun incrementLikedSongs(userId: Int)

    @Query("UPDATE user SET likedSongs = likedSongs - 1 WHERE id = :userId")
    suspend fun decrementLikedSongs(userId: Int)

    @Query("UPDATE user SET songs = songs - 1 WHERE id = :userId")
    suspend fun decrementSongs(userId: Int)

    @Query("UPDATE user SET listenedSongs = listenedSongs - 1 WHERE id = :userId")
    suspend fun decrementListenedSongs(userId: Int)


}