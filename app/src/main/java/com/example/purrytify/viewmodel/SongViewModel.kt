package com.example.purrytify.viewmodel

import android.R
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.model.Song
import com.example.purrytify.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class SongViewModel(private val repository: SongRepository,userId: Int) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _liked_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _current_song = MutableStateFlow<Song?>(null)
    private val _new_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _recently_played = MutableStateFlow<List<Song>>(emptyList())

    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    val likedSongs : StateFlow<List<Song>> = _liked_songs.asStateFlow()
    val current_song : StateFlow<Song?> = _current_song.asStateFlow()
    val newSongs : StateFlow<List<Song>> = _new_songs.asStateFlow()
    val recentlyPlayed : StateFlow<List<Song>> = _recently_played.asStateFlow()

    init {

        loadSongs(userId)
    }

    fun addSong(song: Song) {
        viewModelScope.launch {
            repository.insertSong(song)
            loadSongs(song.userId)
        }
    }

    fun reset() {
        _songs.value = emptyList()
        _liked_songs.value = emptyList()
        _current_song.value = null
        _new_songs.value = emptyList()
        _recently_played.value = emptyList()
    }

    fun deleteSong(song:Song) {
        viewModelScope.launch {
            repository.deleteSong(song.id)
            loadSongs(song.userId)
        }
    }

    fun loadSongs(userId:Int) {
        Log.d("SongViewModel", "Loading songs for userId: $userId")
        viewModelScope.launch {
            repository.getAllSongs(userId).collect { songList ->
                _songs.value = songList

                // Update _current_song jika ID-nya masih ada di list
                _current_song.value?.let { current ->
                    val updated = songList.find { it.id == current.id }
                    _current_song.value = updated
                }
            }
        }

        viewModelScope.launch {
            repository.getAllLikedSongs(userId).collect { _liked_songs.value = it }
        }

        viewModelScope.launch {
            repository.getNewSongs(userId).collect { _new_songs.value = it }
        }

        viewModelScope.launch {
            repository.getRecentlyPlayed(userId).collect { _recently_played.value = it }
        }
    }


    fun toggleLikeSong(song : Song){
        viewModelScope.launch {
            repository.toggleLike(song.id)
            // Fix: Tambahkan null-safety check
            val updatedSong = repository.getSong(song.id)
            if (updatedSong != null) {
                Log.d("SongViewModel", "Updated liked status: ${updatedSong.liked}")
            } else {
                Log.d("SongViewModel", "Song not found in database after toggle")
            }
            Log.d("SongViewModel", "Updated liked songs: ${likedSongs.value}")
            loadSongs(song.userId)
        }
    }

    fun updateSong(song: Song){
        val id = song.id
        val newArtist = song.artist
        val newTitle = song.title
        val newArtwork = song.artworkPath
        viewModelScope.launch {
            repository.updateSong(id,newArtist,newTitle,newArtwork)
            loadSongs(song.userId)
        }
    }

    fun setCurrentSong(song: Song) {
        if (song == current_song.value) {
            return
        }
        viewModelScope.launch {
            try {
                val dbSong = repository.getSong(song.id)
                
                if (dbSong != null) {
                    if (dbSong.lastPlayed == null) {
                        repository.incrementListenedSongs(song.userId)
                    }
                    repository.updateLastPlayed(song.id, Date())
                    loadSongs(song.userId)
                    // Fix: Tambahkan null-safety check juga di sini
                    _current_song.value = repository.getSong(song.id) ?: song
                } else {
                    _current_song.value = song
                    Log.d("SongViewModel", "Setting online song as current: ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error setting current song", e)
                _current_song.value = song
            }
        }
    }

    fun updateLastPlayed(song:Song){
        viewModelScope.launch {
            repository.updateLastPlayed(song.id,Date())
            loadSongs(song.userId)
        }
    }



}