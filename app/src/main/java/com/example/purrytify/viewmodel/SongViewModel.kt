package com.example.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.SongRepository
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongViewModel(private val repository: SongRepository) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _liked_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _newSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    val likedSongs : StateFlow<List<Song>> = _liked_songs.asStateFlow()
    val newSongs : StateFlow<List<Song>> = _newSongs.asStateFlow()
    val recentlyPlayed : StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    init {
        loadSongs()
    }

    fun addSong(song: Song) {
        viewModelScope.launch {
            repository.insertSong(song)
            loadSongs()
        }
    }

    fun deleteSong(songId: Int) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            loadSongs()
        }
    }


    private fun loadSongs() {
        viewModelScope.launch {
        repository.getAllSongsOrdered().collect { _songs.value = it }
    }
        viewModelScope.launch {
            repository.getAllLikedSongs().collect { _liked_songs.value = it }
        }
    }

    fun toggleLikeSong(song : Song){
        viewModelScope.launch {
            repository.toggleLike(song.id)
            val updatedSong = repository.getSong(song.id)
//            println("Updated liked status: ${updatedSong.liked}")
            Log.d("SongViewModel", "Updated liked status: ${updatedSong.liked}")
            Log.d("SongViewModel", "Updated liked songs: ${likedSongs.value}")
            loadSongs()

        }
        viewModelScope.launch {
            repository.getNewSongs().collect { _newSongs.value = it}
        }
        viewModelScope.launch {
            repository.getRecentlyPlayed().collect { _recentlyPlayed.value = it}
        }
    }

//    fun getSong(songId:Int){
//        viewModelScope.launch{
//            repository.getSong(songId)
//        }
//    }

//    fun updateLastPlayed()


}
