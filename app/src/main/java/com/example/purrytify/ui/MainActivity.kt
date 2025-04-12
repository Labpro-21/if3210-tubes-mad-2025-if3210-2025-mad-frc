package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.data.SongRepository
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.ui.InsertSongPopUp
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.screens.LibraryScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val songDao = AppDatabase.getDatabase(applicationContext).songDao()
//        val repository = SongRepository(songDao)
//        val viewModelFactory = SongViewModelFactory(repository)
//        val songViewModel = ViewModelProvider(this, viewModelFactory).get(SongViewModel::class.java)

        enableEdgeToEdge()

//        setContent {
//            PurrytifyTheme {
//                InsertSongPopUp(songViewModel = songViewModel)
//                LibraryScreen()
//            }
        setContent {
            PurrytifyTheme {
                AppNavigation()
            }
        }
    }
}