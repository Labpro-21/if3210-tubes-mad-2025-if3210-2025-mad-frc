package com.example.purrytify.repository.

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Membuat DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_prefs")

@Singleton
class PlayerPreferencesRepository @Inject constructor(
    private val context: Context
) {

    private val dataStore = context.dataStore

    private val currentSongKey = stringPreferencesKey("current_song")
    private val isLoopingKey = booleanPreferencesKey("is_looping")
    private val isShufflingKey = booleanPreferencesKey("is_shuffling")

    // Save current song (URI or song ID)
    suspend fun saveCurrentSong(songUri: String) {
        dataStore.edit { preferences ->
            preferences[currentSongKey] = songUri
        }
    }

    // Get current song (URI or song ID)
    val currentSong: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[currentSongKey]
        }

    // Save looping status
    suspend fun saveLooping(isLooping: Boolean) {
        dataStore.edit { preferences ->
            preferences[isLoopingKey] = isLooping
        }
    }

    // Get looping status
    val isLooping: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[isLoopingKey] ?: false
        }

    // Save shuffling status
    suspend fun saveShuffling(isShuffling: Boolean) {
        dataStore.edit { preferences ->
            preferences[isShufflingKey] = isShuffling
        }
    }

    // Get shuffling status
    val isShuffling: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[isShufflingKey] ?: false
        }
}
