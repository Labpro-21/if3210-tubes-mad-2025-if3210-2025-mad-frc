package com.example.purrytify.viewmodel

import android.R
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
    private val _current_song = MutableStateFlow<Song?>(null)

    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    val likedSongs : StateFlow<List<Song>> = _liked_songs.asStateFlow()
    val current_song : StateFlow<Song?> = _current_song.asStateFlow()

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
            repository.getAllSongsOrdered().collect { songList ->
                _songs.value = songList

                // Update _current_song jika ID-nya masih ada di list
                _current_song.value?.let { current ->
                    val updated = songList.find { it.id == current.id }
                    _current_song.value = updated
                }
            }
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
    }

    fun updateSong(song: Song){
        val id = song.id
        val newArtist = song.artist
        val newTitle = song.title
        val newArtwork = song.artworkPath
        viewModelScope.launch {
            repository.updateSong(id,newArtist,newTitle,newArtwork)
            loadSongs()
        }
    }

    fun setCurrentSong(song:Song){
        _current_song.value = song
    }



}
