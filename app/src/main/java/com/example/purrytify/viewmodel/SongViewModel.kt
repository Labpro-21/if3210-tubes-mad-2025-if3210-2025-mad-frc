package com.example.purrytify.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.utils.MusicServiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class SongViewModel(
    private val repository: SongRepository,
    private val userId: Int,
    private val context: Application,
) : ViewModel() {
    private val userIdToLoad = userId
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _liked_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _new_songs = MutableStateFlow<List<Song>>(emptyList())
    private val _recently_played = MutableStateFlow<List<Song>>(emptyList())


    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    val likedSongs: StateFlow<List<Song>> = _liked_songs.asStateFlow()
    val newSongs: StateFlow<List<Song>> = _new_songs.asStateFlow()
    val recentlyPlayed: StateFlow<List<Song>> = _recently_played.asStateFlow()
    val isPlaying = MusicServiceManager.isPlaying
    var currentSong: StateFlow<Song?> = MusicServiceManager.currentSong


    init {
        loadSongs(this.userId)
        observeIsPlaying()
    }

    fun addSong(song: Song) {
        viewModelScope.launch {
            val songToInsert = song.copy(
                id = 0,
                userId = this@SongViewModel.userId,
                isExplicitlyAdded = true
            )
            repository.insertSong(songToInsert)
            loadSongs(this@SongViewModel.userId)
        }
        sendSongsToMusicService()
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
            val songIdToDelete = song.id
            repository.deleteSong(songIdToDelete)
            if (currentSong.value?.id == songIdToDelete) {
                MusicServiceManager.updateCurrentSong(null)

            }
            loadSongs(song.userId)
        }
    }

    fun sendSongsToMusicService() {
        val songList = _songs.value
        Log.d("SongViewModel", "Sending Playlist of ${songList.size} length")
        val intent = Intent(context, com.example.purrytify.service.MusicService::class.java).apply {
            action = "ACTION_SET_PLAYLIST"
            putParcelableArrayListExtra("playlist", ArrayList(songList))
            Log.d("SongViewModel", "Sending Playlist of ${ArrayList(songList).size} length")
        }
        context.startService(intent)
    }

    fun loadSongs(userIdToLoad:Int) {
        Log.d("SongViewModel_loadSongs", "Loading songs for userId: $userIdToLoad")
        viewModelScope.launch {
            repository.getAllExplicitlyAddedSongs(userIdToLoad).collect { songList ->
                _songs.value = songList


                MusicServiceManager.currentSong.value?.let { current ->
                    val updated = songList.find { it.id == current.id }
                    if (updated != null) {
                        MusicServiceManager.updateCurrentSong(updated)
                    }
                }
            }
        }
        
        viewModelScope.launch {
            repository.getAllLikedSongs(userIdToLoad).collect { likedList ->
                _liked_songs.value = likedList
            }
        }

        viewModelScope.launch {
            repository.getAllSongsInternal(userIdToLoad).collect { allSongsList ->
                val currentSongSnapshot = currentSong.value
                if (currentSongSnapshot != null && currentSongSnapshot.id != 0) {
                    val songFromDbList =
                        allSongsList.find { it.id == currentSongSnapshot.id && it.userId == currentSongSnapshot.userId }
                    if (songFromDbList != null) {
                        if (currentSongSnapshot.liked != songFromDbList.liked ||
                            currentSongSnapshot.title != songFromDbList.title ||
                            currentSongSnapshot.artist != songFromDbList.artist ||
                            currentSongSnapshot.artworkPath != songFromDbList.artworkPath
                        ) {
                            Log.d(
                                "SongViewModel_loadSongs",
                                "Updating _current_song (ID: ${currentSongSnapshot.id}) due to changes in DB (e.g., liked status)."
                            )
                            MusicServiceManager.updateCurrentSong(songFromDbList)
                        }
                    } else {
                        Log.d(
                            "SongViewModel_loadSongs",
                            "Current song (ID: ${currentSongSnapshot.id}) not found in allSongsList. setCurrentSong should handle if it was deleted."
                        )
                    }
                }

            }
        }
        viewModelScope.launch {
            repository.getAllLikedSongs(userIdToLoad).collect { likedsongList ->
                _liked_songs.value = likedsongList
            }
        }
        viewModelScope.launch {
            repository.getRecentlyPlayed(userIdToLoad).collect { recentsongList ->
                _recently_played.value = recentsongList
            }
        }
        viewModelScope.launch {
            repository.getNewSongs(userIdToLoad).collect { newsongList ->
                _new_songs.value = newsongList
            }
        }
    }
    fun setCurrentSong(songData: Song) {
        viewModelScope.launch {
            val effectiveUserId = this@SongViewModel.userId
            val previousSongPath = currentSong.value?.audioPath
            Log.i("SongViewModel_setCurrentSong", "START: Setting current song: ${songData.title} (Original ID: ${songData.id}, Path: ${songData.audioPath}) for user $effectiveUserId")

            var fullyProcessedSong: Song? = null

            if (songData.audioPath.startsWith("http")) {
                var localCopy = repository.getSongByAudioPathAndUserId(songData.audioPath, effectiveUserId)
                if (localCopy == null) {


                    val newEntry = songData.copy(id = 0, userId = effectiveUserId, addedDate = Date(), lastPlayed = null, liked = false, isExplicitlyAdded = false)
                    val newLocalId = repository.insertSong(newEntry)
                    if (newLocalId > 0) {
                        localCopy = repository.getSong(newLocalId.toInt())
                        if (localCopy != null) {
                            Log.i("SongViewModel_setCurrentSong", "Online song inserted. Local ID: ${localCopy.id}, Title: ${localCopy.title}")
                            fullyProcessedSong = localCopy
                        } else {
                            Log.e("SongViewModel_setCurrentSong", "CRITICAL ERROR: Failed to retrieve song from DB after insert. Generated ID: $newLocalId")
                        }
                    } else {
                        Log.e("SongViewModel_setCurrentSong", "CRITICAL ERROR: InsertSong returned invalid ID ($newLocalId) for online song.")
                    }
                } else {
                    Log.i("SongViewModel_setCurrentSong", "Online song '${songData.title}' found in local DB. Local ID: ${localCopy.id}")
                    fullyProcessedSong = localCopy
                }
            } else {
                val localSongFromDb = repository.getSong(songData.id)
                if (localSongFromDb != null && localSongFromDb.userId == effectiveUserId) {
                    Log.i("SongViewModel_setCurrentSong", "Local song '${localSongFromDb.title}' (ID: ${localSongFromDb.id}) found.")
                    fullyProcessedSong = localSongFromDb
                } else {
                    Log.e("SongViewModel_setCurrentSong", "Local song ID ${songData.id} not found or wrong user.")
                }
            }


            if (fullyProcessedSong != null && fullyProcessedSong.id != 0) {
                val isFirstTimePlayedLocally = fullyProcessedSong.lastPlayed == null
                repository.updateLastPlayed(fullyProcessedSong.id, Date())
                if (isFirstTimePlayedLocally) {
                    repository.incrementListenedSongs(fullyProcessedSong.userId)
                }
                MusicServiceManager.updateCurrentSong(repository.getSong(fullyProcessedSong.id))
                if (currentSong.value != null) {
                    Log.i("SongViewModel_setCurrentSong", "SUCCESS: currentSong.value set to ID: ${currentSong.value!!.id}, Title: ${currentSong.value!!.title}")
                } else {
                    Log.e("SongViewModel_setCurrentSong", "ERROR: currentSong.value became null after fetching final song ID ${fullyProcessedSong.id}")
                }
            } else {
                Log.e("SongViewModel_setCurrentSong", "Failed to obtain valid local song for ${songData.title}. Setting currentSong to null.")
                MusicServiceManager.updateCurrentSong(null)
            }

            loadSongs(effectiveUserId)
            Log.i("SongViewModel_setCurrentSong", "END: setCurrentSong for ${songData.title}. Final currentSong: ${currentSong.value?.title}")
        }
    }
    fun toggleLikeSong(song : Song) {
        viewModelScope.launch {
            if (song.id != 0 && song.userId == this@SongViewModel.userId) {
                val songFromDb = repository.getSong(song.id)
                if (songFromDb != null) {
                    repository.toggleLike(songFromDb.id)
                    val updatedSong = repository.getSong(songFromDb.id)
                    if (currentSong.value?.id == songFromDb.id) {
                        MusicServiceManager.updateCurrentSong(updatedSong)
                    }
                    loadSongs(songFromDb.userId)
                } else {
                    Log.w("SongViewModel_toggleLike", "Song with id ${song.id} not found in DB.")
                }
            } else {
                Log.w("SongViewModel_toggleLike", "Attempted to toggle like on a song not properly set for current user or invalid ID (ID: ${song.id}, User: ${song.userId}, CurrentUser: ${this@SongViewModel.userId})")
            }
        }
    }

    fun getSongIndex(song: Song): Int {
        return _songs.value.indexOf(song)
    }

    fun updateSong(songFromPopup: Song){
        viewModelScope.launch {
            if (songFromPopup.userId == this@SongViewModel.userId && songFromPopup.id != 0) {
                repository.updateSong(
                    songFromPopup.id,
                    songFromPopup.artist,
                    songFromPopup.title,
                    songFromPopup.artworkPath,
                    true
                )
                loadSongs(songFromPopup.userId)
            }
        }
        sendSongsToMusicService()
    }




    fun updateLastPlayed(song:Song){
        viewModelScope.launch {
            if (song.id != 0 && song.userId == this@SongViewModel.userId) {
                Log.d("SongViewModel_updateLastPlayed", "Updating lastPlayed for songId: ${song.id}")
                repository.updateLastPlayed(song.id,Date())
                if (currentSong.value?.id == song.id) {
                    MusicServiceManager.updateCurrentSong(repository.getSong(song.id))
                }
            }
        }
    }

    fun recordPlayTick() {
        viewModelScope.launch {
            currentSong.value?.let { currentSong ->

                if (currentSong.id != 0 && currentSong.userId == this@SongViewModel.userId) {
                    Log.d("SongViewModel_recordPlayTick", "Attempting tick. Current Song ID: ${currentSong.id}, Title: ${currentSong.title}, UserID: ${currentSong.userId}, Path: ${currentSong.audioPath}")
                    val history = PlayHistory(
                        user_id = this@SongViewModel.userId,
                        played_at = Date(),
                        duration_ms = 1_000L,
                        song_id = currentSong.id
                    )
                    try {
                        repository.addPlayHistory(history)
                        Log.d("SongViewModel_recordPlayTick", "Successfully recorded play tick for local songId: ${currentSong.id}")
                    } catch (e: Exception) {
                        Log.e("SongViewModel_recordPlayTick", "Error recording play tick for songId ${currentSong.id}: ${e.message}", e)
                    }
                } else {
                    Log.w("SongViewModel_recordPlayTick", "Skipped: Invalid song ID (${currentSong.id}) or mismatched user (${currentSong.userId} vs ${this@SongViewModel.userId}).")
                }
            } ?: run {
                Log.w("SongViewModel_recordPlayTick", "Skipped recording play tick because currentSong is null.")
            }
        }
    }

    fun observeIsPlaying(){
        viewModelScope.launch {
            isPlaying.collect { playing ->
                if (playing) {
                    while (isPlaying.value) {
                        recordPlayTick()
                        delay(1000L)
                    }
                }
            }
        }
    }
    suspend fun isDownloadedByServerId(serverId: Int?): Boolean = repository.isDownloadedByServerId(serverId)
}

