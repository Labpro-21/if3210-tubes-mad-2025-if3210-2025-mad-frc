package com.example.purrytify.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "song",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)

@Parcelize
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ID Lokal
    @ColumnInfo(name = "server_id", index = true) val serverId: Int? = null, // ID Asli dari Server (nullable)
    @ColumnInfo(name="title") val title: String="Unnamed Song",
    @ColumnInfo(name="artist") val artist: String="Unnamed Artist",
    @ColumnInfo(name="duration") val duration: Long,
    @ColumnInfo(name="artworkPath") val artworkPath: String?,
    @ColumnInfo(name="audioPath") val audioPath: String,
    @ColumnInfo(name="lastPlayed") val lastPlayed: Date?,
    @ColumnInfo(name="addedDate") val addedDate: Date,
    @ColumnInfo(name="liked") val liked: Boolean=false,
    @ColumnInfo(name="user_id") val userId: Int=0,
    @ColumnInfo(name="isExplicitlyAdded") val isExplicitlyAdded: Boolean = false
): Parcelable

