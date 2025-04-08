package com.example.purrytify.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName= "song")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    @ColumnInfo(name="title") val title: String,
    @ColumnInfo(name="artist") val artist: String,
    @ColumnInfo(name="duration") val duration: Long,
    @ColumnInfo(name="artworkPath") val artworkPath: String?,
    @ColumnInfo(name="audioPath") val audioPath: String
)
