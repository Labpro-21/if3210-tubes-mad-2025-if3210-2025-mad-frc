package com.example.purrytify.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.R
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.SongRepository
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.example.purrytify.ui.navBar.BottomNavBar
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
fun HomeScreenContent(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel
) {
    val context = LocalContext.current
    val appContext = if (!LocalInspectionMode.current)
        context.applicationContext as? Application
    else null

    if (appContext == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Preview Mode - AppContext tidak tersedia", color = Color.White)
        }
        return
    }

    // ViewModel untuk player
    val newSongsFromDb by songViewModel.newSongs.collectAsState()
    val recentlyPlayedFromDb by songViewModel.recentlyPlayed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Bagian New Songs dari database
        Text(
            text = "New songs",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (newSongsFromDb.isEmpty()) {
            // Tampilkan pesan fallback untuk New Songs
            Text(
                text = "No new songs yet",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyRow {
                items(newSongsFromDb) { song ->
                    Column(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                            contentDescription = "Artwork",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Bagian Recently Played dari database
        Text(
            text = "Recently played",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (recentlyPlayedFromDb.isEmpty()) {
            // Tampilkan pesan fallback untuk Recently Played
            Text(
                text = "No recently played songs, start listening now",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Column {
                recentlyPlayedFromDb.forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                            contentDescription = song.title,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = song.artist,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomPlayerSection(
    isPlaying: Boolean = true,
    onPlayPause: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = if (!LocalInspectionMode.current)
        context.applicationContext as? Application
    else null

    if (appContext == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Preview Mode - Player tidak aktif", color = Color.White)
        }
        return
    }
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(appContext))
    // Jika Anda sudah memiliki onPlayPause, Anda bisa gunakan callback onPlayPause di sini.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.album_cover1),
            contentDescription = "Currently Playing",
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Starboy",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "The Weeknd",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        IconButton(
            onClick = { onPlayPause() },
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun HomeScreenWithBottomNav(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = PlayerViewModelFactory(
            (LocalContext.current as Activity).application
        )
    ),
    isPlaying: Boolean = true,
    onPlayPause: () -> Unit = { playerViewModel.playPause() }
) {
    Scaffold(
        bottomBar = {
            Column {
                // Bottom player section yang mengambil current song dari database via SongViewModel
                BottomPlayerSectionFromDB(
                    songViewModel = songViewModel,
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause
                )
                // Bottom navigation bar (reusable)
                BottomNavBar(
                    currentRoute = "home",
                    onItemSelected = { route ->
                        when (route) {
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                            // "home" sudah aktif
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Konten utama HomeScreen
            HomeScreenContent(
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToProfile = onNavigateToProfile,
                songViewModel = songViewModel
            )
        }
    }
}