package com.example.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class SongViewModel(private val repository: SongRepository, private val userId: Int) : ViewModel() {

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
        loadSongs(this.userId)
    }

    fun addSong(song: Song) {
        viewModelScope.launch {
            val songToInsert = song.copy(
                id = if (song.id == 0) 0 else song.id,
                userId = this@SongViewModel.userId
            )
            repository.insertSong(songToInsert)
            loadSongs(this@SongViewModel.userId)
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

    fun loadSongs(userIdToLoad:Int) { // Ganti nama parameter agar tidak ambigu
        Log.d("SongViewModel", "Loading songs for userId: $userIdToLoad")
        viewModelScope.launch {
            repository.getAllSongs(userIdToLoad).collect { songList ->
                _songs.value = songList
                _current_song.value?.let { current ->
                    val updatedCurrentSong = songList.find { it.id == current.id }
                    if (updatedCurrentSong == null && current.audioPath.startsWith("http")) {
                        // Current song was an online song and might not be in the main list yet
                        // or was deleted. Keep it as is if it's still relevant.
                    } else {
                        _current_song.value = updatedCurrentSong
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.getAllLikedSongs(userIdToLoad).collect { _liked_songs.value = it }
        }

        viewModelScope.launch {
            repository.getNewSongs(userIdToLoad).collect { _new_songs.value = it }
        }

        viewModelScope.launch {
            repository.getRecentlyPlayed(userIdToLoad).collect { _recently_played.value = it }
        }
    }


    fun toggleLikeSong(song : Song) {
        viewModelScope.launch {
            if (_current_song.value?.id == song.id && song.id != 0 && song.userId == this@SongViewModel.userId) {
                repository.toggleLike(song.id)
                loadSongs(song.userId)
            } else {
                Log.w("SongViewModel", "Attempted to toggle like on a song not properly set for current user.")
            }
        }
    }

    fun updateSong(song: Song){
        val id = song.id
        val newArtist = song.artist
        val newTitle = song.title
        val newArtwork = song.artworkPath
        viewModelScope.launch {
            if (song.userId == this@SongViewModel.userId) {
                repository.updateSong(id,newArtist,newTitle,newArtwork)
                loadSongs(song.userId)
            }
        }
    }

    fun setCurrentSong(songData: Song) {
        viewModelScope.launch {
            val effectiveUserId = this@SongViewModel.userId

            if (songData.audioPath.startsWith("http")) {
                var songToPlay = repository.getSongByAudioPathAndUserId(songData.audioPath, effectiveUserId)

                if (songToPlay == null) {
                    Log.d("SongViewModel", "Online song '${songData.title}' not in local DB for user $effectiveUserId. Inserting with new local ID.")
                    val newLocalSongEntry = songData.copy(
                        id = 0,
                        userId = effectiveUserId,
                        addedDate = Date(),
                        lastPlayed = null,
                        liked = false
                    )
                    val generatedLocalId = repository.insertSong(newLocalSongEntry)
                    songToPlay = repository.getSong(generatedLocalId.toInt())

                    if (songToPlay == null) {
                        Log.e("SongViewModel", "Failed to fetch newly inserted online song from DB with generated ID $generatedLocalId.")
                        _current_song.value = songData
                        loadSongs(effectiveUserId)
                        return@launch
                    }
                    Log.d("SongViewModel", "Inserted online song as local ID: ${songToPlay.id}, Title: ${songToPlay.title}")
                } else {
                    Log.d("SongViewModel", "Online song '${songData.title}' found in local DB for user $effectiveUserId with ID: ${songToPlay.id}.")
                }
                _current_song.value = songToPlay
                if (songToPlay.lastPlayed == null) {
                    repository.incrementListenedSongs(songToPlay.userId)
                }
                repository.updateLastPlayed(songToPlay.id, Date())

            } else { // Local song
                val localSongFromDb = repository.getSong(songData.id)
                // Memastikan lagu milik user yang sedang aktif
                if (localSongFromDb != null && localSongFromDb.userId == effectiveUserId) {
                    if (localSongFromDb.lastPlayed == null) {
                        repository.incrementListenedSongs(localSongFromDb.userId)
                    }
                    repository.updateLastPlayed(localSongFromDb.id, Date())
                    _current_song.value = repository.getSong(localSongFromDb.id)
                } else {
                    Log.e("SongViewModel", "Local song with id ${songData.id} not found or doesn't belong to user $effectiveUserId.")
                    _current_song.value = if(localSongFromDb?.userId == effectiveUserId) localSongFromDb else null
                }
            }
            loadSongs(effectiveUserId)
        }
    }

    fun updateLastPlayed(song:Song){
        viewModelScope.launch {
            if (song.id != 0 && song.userId == this@SongViewModel.userId) {
                repository.updateLastPlayed(song.id,Date())
                loadSongs(song.userId)
            }
        }
    }

    fun recordPlayTick() {
        viewModelScope.launch {
            _current_song.value?.let { currentSong ->
                if (currentSong.id != 0 && currentSong.userId == this@SongViewModel.userId) {
                    val history = PlayHistory(
                        user_id = this@SongViewModel.userId, // Menggunakan userId dari properti kelas
                        played_at = Date(),
                        duration_ms = 1_000L,
                        song_id = currentSong.id
                    )
                    repository.addPlayHistory(history)
                    Log.d("SongViewModel", "Recorded play tick for songId: ${currentSong.id}")
                } else {
                    Log.w("SongViewModel", "Skipped recording play tick for song without valid local ID or wrong user: ${currentSong.title}, id: ${currentSong.id}, userId: ${currentSong.userId}, vmUserId: ${this@SongViewModel.userId}")
                }
            }
        }
    }
}