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
                id = 0,
                userId = this@SongViewModel.userId,
                isExplicitlyAdded = true
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
            val songIdToDelete = song.id
            repository.deleteSong(songIdToDelete)
            if (_current_song.value?.id == songIdToDelete) {
                _current_song.value = null
                // Anda mungkin juga ingin menghentikan PlayerViewModel di sini jika lagu yang dihapus sedang diputar
            }
            loadSongs(song.userId)
        }
    }

    fun loadSongs(userIdToLoad:Int) {
        Log.d("SongViewModel_loadSongs", "Loading songs for userId: $userIdToLoad")
        viewModelScope.launch {
            repository.getAllExplicitlyAddedSongs(userIdToLoad).collect { songList ->
                _songs.value = songList
            }
        }

        viewModelScope.launch {
            repository.getAllLikedSongs(userIdToLoad).collect { likedList ->
                _liked_songs.value = likedList
            }
        }

        viewModelScope.launch {
        repository.getAllSongsInternal(userIdToLoad).collect { allSongsList ->
             val currentSongSnapshot = _current_song.value
             if (currentSongSnapshot != null && currentSongSnapshot.id != 0) { // Hanya jika ada current song yang valid
                val songFromDbList = allSongsList.find { it.id == currentSongSnapshot.id && it.userId == currentSongSnapshot.userId }
                if (songFromDbList != null) {
                    // Hanya update _current_song jika ada perubahan relevan (misal status liked, atau jika objeknya berbeda instance)
                    // dan BUKAN jika setCurrentSong baru saja mengaturnya.
                    // Untuk menghindari race condition, mungkin lebih baik jika setCurrentSong adalah satu-satunya yang mengatur _current_song secara langsung
                    // dan loadSongs hanya memicu refresh data lagu lain.
                    // Atau, pastikan objeknya benar-benar berbeda sebelum update.
                    if (currentSongSnapshot.liked != songFromDbList.liked ||
                        currentSongSnapshot.title != songFromDbList.title || // Contoh field lain jika bisa berubah
                        currentSongSnapshot.artist != songFromDbList.artist ||
                        currentSongSnapshot.artworkPath != songFromDbList.artworkPath) {
                       Log.d("SongViewModel_loadSongs", "Updating _current_song (ID: ${currentSongSnapshot.id}) due to changes in DB (e.g., liked status).")
                       _current_song.value = songFromDbList
                    }
                } else {
                    // Jika lagu yang sebelumnya current sudah tidak ada di DB (dan bukan online song yang baru diproses setCurrentSong)
                    // Kita bisa null-kan _current_song jika memang sudah dihapus.
                    // Namun, setCurrentSong yang baru saja dijalankan dari deep link akan menangani ini.
                    // Jadi, mungkin tidak perlu null-kan di sini jika setCurrentSong sudah benar.
                    Log.d("SongViewModel_loadSongs", "Current song (ID: ${currentSongSnapshot.id}) not found in allSongsList. setCurrentSong should handle if it was deleted.")
                }
             }
        }
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
            if (song.id != 0 && song.userId == this@SongViewModel.userId) {
                 val songFromDb = repository.getSong(song.id)
                 if (songFromDb != null) {
                    repository.toggleLike(songFromDb.id)
                    val updatedSong = repository.getSong(songFromDb.id)
                    if (_current_song.value?.id == songFromDb.id) {
                        _current_song.value = updatedSong
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
    }

    fun setCurrentSong(songData: Song) {
        viewModelScope.launch {
            val effectiveUserId = this@SongViewModel.userId
            val previousSongPath = _current_song.value?.audioPath // Simpan path lagu sebelumnya
            Log.i("SongViewModel_setCurrentSong", "START: Setting current song: ${songData.title} (Original ID: ${songData.id}, Path: ${songData.audioPath}) for user $effectiveUserId")

            var fullyProcessedSong: Song? = null

            // ... (Logika untuk mendapatkan fullyProcessedSong dari DB atau insert baru, SAMA SEPERTI SEBELUMNYA)
            // Ini adalah logika inti untuk memastikan fullyProcessedSong valid dari DB dengan ID lokal.
                if (songData.audioPath.startsWith("http")) {
                    var localCopy = repository.getSongByAudioPathAndUserId(songData.audioPath, effectiveUserId)
                    if (localCopy == null) {
                        // ... (insert new online song, get localCopy with local ID) ...
                        // (Pastikan localCopy di-set di sini jika berhasil)
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
                } else { // Local song
                    val localSongFromDb = repository.getSong(songData.id)
                    if (localSongFromDb != null && localSongFromDb.userId == effectiveUserId) {
                        Log.i("SongViewModel_setCurrentSong", "Local song '${localSongFromDb.title}' (ID: ${localSongFromDb.id}) found.")
                        fullyProcessedSong = localSongFromDb
                    } else {
                        Log.e("SongViewModel_setCurrentSong", "Local song ID ${songData.id} not found or wrong user.")
                    }
                }
            // Akhir dari logika mendapatkan fullyProcessedSong

            if (fullyProcessedSong != null && fullyProcessedSong.id != 0) {
                val isFirstTimePlayedLocally = fullyProcessedSong.lastPlayed == null
                repository.updateLastPlayed(fullyProcessedSong.id, Date())
                if (isFirstTimePlayedLocally) {
                    repository.incrementListenedSongs(fullyProcessedSong.userId)
                }
                // Set _current_song dengan instance yang sudah diproses dan divalidasi
                _current_song.value = repository.getSong(fullyProcessedSong.id) // Re-fetch untuk data paling baru
                if (_current_song.value != null) {
                    Log.i("SongViewModel_setCurrentSong", "SUCCESS: _current_song.value set to ID: ${_current_song.value!!.id}, Title: ${_current_song.value!!.title}")
                } else {
                    Log.e("SongViewModel_setCurrentSong", "ERROR: _current_song.value became null after fetching final song ID ${fullyProcessedSong.id}")
                }
            } else {
                Log.e("SongViewModel_setCurrentSong", "Failed to obtain valid local song for ${songData.title}. Setting _current_song to null.")
                _current_song.value = null // Jika tidak ada lagu valid yang bisa diproses, _current_song harus null
            }

            loadSongs(effectiveUserId) // Panggil loadSongs setelah _current_song di-set
            Log.i("SongViewModel_setCurrentSong", "END: setCurrentSong for ${songData.title}. Final _current_song: ${_current_song.value?.title}")
        }
    }


    fun updateLastPlayed(song:Song){
        viewModelScope.launch {
            if (song.id != 0 && song.userId == this@SongViewModel.userId) {
                Log.d("SongViewModel_updateLastPlayed", "Updating lastPlayed for songId: ${song.id}")
                repository.updateLastPlayed(song.id,Date())
                 if (_current_song.value?.id == song.id) {
                     _current_song.value = repository.getSong(song.id)
                 }
            }
        }
    }

    fun recordPlayTick() {
        viewModelScope.launch {
            _current_song.value?.let { currentSong ->
                // Kondisi paling penting: ID harus valid (bukan 0) dan milik user saat ini.
                if (currentSong.id != 0 && currentSong.userId == this@SongViewModel.userId) {
                    Log.d("SongViewModel_recordPlayTick", "Attempting tick. Current Song ID: ${currentSong.id}, Title: ${currentSong.title}, UserID: ${currentSong.userId}, Path: ${currentSong.audioPath}")
                    val history = PlayHistory(
                        user_id = this@SongViewModel.userId,
                        played_at = Date(),
                        duration_ms = 1_000L,
                        song_id = currentSong.id // Ini harus ID lokal yang valid
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
                Log.w("SongViewModel_recordPlayTick", "Skipped recording play tick because _current_song is null.")
            }
        }
    }
}