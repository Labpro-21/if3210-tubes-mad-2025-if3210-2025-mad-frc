package com.example.purrytify.model;

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
  tableName = "play_history",
  foreignKeys = [
    ForeignKey(entity = Song::class,
               parentColumns = ["id"],
               childColumns = ["song_id"],
               onDelete = ForeignKey.CASCADE)
  ]
)

data class PlayHistory(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val song_id: Int,
  val user_id: Int,
  val played_at: Date,
  val duration_ms: Long
)