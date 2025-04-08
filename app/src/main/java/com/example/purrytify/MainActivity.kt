package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.data.SongRepository
import com.example.purrytify.manager.AppDatabase
import com.example.purrytify.ui.InsertSongPopUp
import com.example.purrytify.ui.SongScreen
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songDao = AppDatabase.getDatabase(applicationContext).songDao()
        val repository = SongRepository(songDao)
        val viewModelFactory = SongViewModelFactory(repository)
        val songViewModel = ViewModelProvider(this, viewModelFactory).get(SongViewModel::class.java)

        enableEdgeToEdge()

        setContent {
            PurrytifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SongScreen(modifier = Modifier.padding(innerPadding))
                }
                InsertSongPopUp(songViewModel = songViewModel)
            }
        }
    }
}