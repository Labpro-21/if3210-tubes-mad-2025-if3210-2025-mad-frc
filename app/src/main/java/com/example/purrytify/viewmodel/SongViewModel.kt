package com.example.purrytify.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.example.purrytify.model.Song
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.utils.MusicServiceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class SongViewModel(
    private val repository: SongRepository,
    userId: Int,
    private val context: Application,
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _liked_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _new_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _recently_played = MutableStateFlow<List<Song>>(emptyList())

    // Expose songs as StateFlow
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    val likedSongs: StateFlow<List<Song>> = _liked_songs.asStateFlow()
    val newSongs: StateFlow<List<Song>> = _new_songs.asStateFlow()
    val recentlyPlayed: StateFlow<List<Song>> = _recently_played.asStateFlow()

    val current_song: StateFlow<Song?> = MusicServiceManager.currentSong

    private val _currentSongLiveData = MutableLiveData<Song?>()
    val currentSongLiveData: LiveData<Song?> = _currentSongLiveData

    init {
        viewModelScope.launch {
            MusicServiceManager.currentSong.collect { song ->
                _currentSongLiveData.postValue(song)
            }
        }

        loadSongs(userId)

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

    fun addSong(song: Song) {
        viewModelScope.launch {
            repository.insertSong(song)
            loadSongs(song.userId)
        }
    }

    fun reset() {
        _songs.value = emptyList()
        _liked_songs.value = emptyList()
        _new_songs.value = emptyList()
        _recently_played.value = emptyList()
        MusicServiceManager.updateCurrentSong(null)
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song.id)
            loadSongs(song.userId)
        }
    }

    private fun sendSongsToMusicService() {
        val songList = _songs.value
        Log.d("SongViewModel", "Sending Playlist of ${songList.size} length")
        val intent = Intent(context, com.example.purrytify.service.MusicService::class.java).apply {
            action = "ACTION_SET_PLAYLIST"
            putParcelableArrayListExtra("playlist", ArrayList(songList))
            Log.d("SongViewModel", "Sending Playlist of ${ArrayList(songList).size} length")
        }
        context.startService(intent)
    }

    fun loadSongs(userId: Int) {
        Log.d("SongViewModel", "Loading songs for userId: $userId")
        viewModelScope.launch {
            repository.getAllSongs(userId).collect { songList ->
                _songs.value = songList

                // Update current song jika masih valid
                MusicServiceManager.currentSong.value?.let { current ->
                    val updated = songList.find { it.id == current.id }
                    if (updated != null) {
                        MusicServiceManager.updateCurrentSong(updated)
                    }
                }

                sendSongsToMusicService()
            }
        }
    }

    fun toggleLikeSong(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song.id)
            val updatedSong = repository.getSong(song.id)
            if (updatedSong != null) {
                Log.d("SongViewModel", "Updated liked status: ${updatedSong.liked}")
            } else {
                Log.d("SongViewModel", "Song not found after toggle")
            }
            loadSongs(song.userId)
        }
    }

    fun updateSong(song: Song) {
        val id = song.id
        val newArtist = song.artist
        val newTitle = song.title
        val newArtwork = song.artworkPath
        viewModelScope.launch {
            repository.updateSong(id, newArtist, newTitle, newArtwork)
            loadSongs(song.userId)
        }
    }

    fun setCurrentSong(song: Song) {
        if (song == MusicServiceManager.currentSong.value) {
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
                    MusicServiceManager.updateCurrentSong(dbSong)
                } else {
                    MusicServiceManager.updateCurrentSong(song)
                    Log.d("SongViewModel", "Setting online song as current: ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error setting current song", e)
                MusicServiceManager.updateCurrentSong(song)
            }
        }
    }

    fun getSongIndex(song: Song): Int {
        return _songs.value.indexOf(song)
    }
}
