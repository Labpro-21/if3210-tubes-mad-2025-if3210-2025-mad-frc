package com.example.purrytify.ui.screens

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.ui.LockScreenOrientation
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.ui.navBar.VerticalNavBar
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.SongViewModel
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.network.ApiService
import com.example.purrytify.network.RetrofitClient
import java.util.Date
import com.example.purrytify.ui.components.SongSettingsModal
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.TokenManager
import com.example.purrytify.utils.shareServerSong
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.OnlineSongViewModelFactory
import com.example.purrytify.viewmodel.RecommendationViewModel

@Composable
fun HomeScreenContent(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    newSongsFromDb: List<Song>,
    recentlyPlayedFromDb: List<Song>,
    onlineSongViewModel: OnlineSongViewModel,
    songVm: SongViewModel,
    onNavigateToTopSong: (String) -> Unit,
    recommendationViewModel: RecommendationViewModel,
    isConnected : Boolean
) {
    val context = LocalContext.current
    val appContext = if (!LocalInspectionMode.current)
        context.applicationContext as? Application
    else null


    var showSongSettings by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    val dailyMixSongs by recommendationViewModel.dailyMix.collectAsState()
    val isLoadingRecommendations by recommendationViewModel.isLoading.collectAsState()
    val currentPlayingSong by songViewModel.currentSong.collectAsState()

    if (appContext == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Preview Mode - AppContext tidak tersedia",
                color = Color.White
            )
        }
        return
    }

    val onlineSongs by onlineSongViewModel.onlineSongs.collectAsState()

    var showNoInternetDialog by remember { mutableStateOf(!isConnected) }

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    LaunchedEffect(Unit) {
        onlineSongViewModel.loadTopSongs(null)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Charts",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                ChartCard(
                    title = "Top 50",
                    subtitle = "GLOBAL",
                    colors = listOf(Color(0xFF0B4870), Color(0xFF16BFFD)),
                    onClick = {
                        if (isConnected) {
                            onNavigateToTopSong("global")
                        } else {
                            showNoInternetDialog = true
                        }
                    }
                )
                

                ChartCard(
                    title = "Top 10",
                    subtitle = "INDONESIA",
                    colors = listOf(Color(0xFFE34981), Color(0xFFFFB25E)),
                    onClick = {
                        if (isConnected){
                            onNavigateToTopSong("id")
                        }else{
                            showNoInternetDialog = true
                        }
                    }
                )
            }
        }


        item {
            Text(
                text = "New songs",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (newSongsFromDb.isEmpty()) {
            item {
                Text(
                    text = "No new songs yet",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newSongsFromDb) { song ->
                        NewSongCard(
                            song = song,
                            onClick = {
                                songViewModel.setCurrentSong(song)
//                                playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                                val index = songViewModel.getSongIndex(song)
                                Log.d("HomeScreen", "Picked SongId: ${index}")
                                songViewModel.sendSongsToMusicService()
                                playerViewModel.prepareAndPlay(index)

                            },
                            onMoreClick = {
                                selectedSong = song
                                showSongSettings = true
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Daily Mix",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isLoadingRecommendations) {
                    IconButton(onClick = { recommendationViewModel.refreshDailyMix() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Mix", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoadingRecommendations) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        } else if (dailyMixSongs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Tidak ada rekomendasi untukmu saat ini. Coba dengarkan lebih banyak lagu!",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(dailyMixSongs, key = { it.audioPath }) { song ->
                        NewSongCard(
                            song = song,
                            onClick = {
                                songViewModel.setCurrentSong(song)
                                recommendationViewModel.sendSongsToMusicService()
                                playerViewModel.prepareAndPlay(dailyMixSongs.indexOf(song))
//                                playerViewModel.prepareAndPlay(song.audioPath.toUri()) {
//
//                                    val currentIndex = dailyMixSongs.indexOf(song)
//                                    if (currentIndex != -1 && currentIndex < dailyMixSongs.size - 1) {
//                                        val nextSong = dailyMixSongs[currentIndex + 1]
//                                        songViewModel.setCurrentSong(nextSong)
//                                        playerViewModel.prepareAndPlay(nextSong.audioPath.toUri()) {/* rekursif atau handle akhir playlist */}
//                                    }
//                                }
                            },
                            onMoreClick = {
                                Toast.makeText(context, "More options for ${song.title}", Toast.LENGTH_SHORT).show()
                            },


                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Recently played",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (recentlyPlayedFromDb.isEmpty()) {
            item {
                Text(
                    text = "No recently played songs, start listening now",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(recentlyPlayedFromDb) { song ->
                RecentlyPlayedRow(
                    song = song,
                    onClick = {
                        songViewModel.setCurrentSong(song)
                        Log.d("HomeScreen", "Picked SongId: ${song.title}")
                        playerViewModel.playSingleSong(song)
                    },
                    onMoreClick = {
                        selectedSong = song
                        showSongSettings = true
                    }
                )
            }
        }
    }


    SongSettingsModal(
        song = selectedSong,
        visible = showSongSettings,
        onDismiss = { showSongSettings = false },
        onEdit = { 
            selectedSong?.let { song ->

            }
         },
        onDelete = { 
            showDeleteDialog = true
         },
        onShareUrl = {
            selectedSong?.let { songToShare ->
                shareServerSong(context, songToShare)
            }
        },
        isOnlineSong = selectedSong?.audioPath?.startsWith("http") == true
    )
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors))
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NewSongCard(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                contentDescription = song.title,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        

        Text(
            text = song.artist,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentlyPlayedRow(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithBottomNav(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    newSongsFromDb: List<Song>,
    recentlyPlayedFromDb: List<Song>,
    onlineSongViewModel: OnlineSongViewModel,
    songVm: SongViewModel,
    onNavigateToTopSong: (String) -> Unit,
    isConnected: Boolean,
    audioOutputViewModel: AudioOutputViewModel,
    recommendationViewModel: RecommendationViewModel
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    var showPlayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )

    if (showPlayerSheet) {
        PlayerModalBottomSheet(
            showSheet = showPlayerSheet,
            onDismiss = { showPlayerSheet = false },
            song = songViewModel.currentSong.collectAsState(initial = null).value ?: return,
            songViewModel = songViewModel,
            onSongChange = { },
            playerViewModel = playerViewModel,
            sheetState = sheetState,
            isOnline = isConnected,
            audioOutputViewModel = audioOutputViewModel
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                BottomPlayerSectionFromDB(
                    songViewModel = songViewModel,
                    isPlaying = isPlaying,
                    onPlayPause = { playerViewModel.playPause() },
                    onSectionClick = { showPlayerSheet = true }
                )
                BottomNavBar(
                    currentRoute = "home",
                    onItemSelected = {
                        when (it) {
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeScreenContent(
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToProfile = onNavigateToProfile,
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
                newSongsFromDb = newSongsFromDb,
                recentlyPlayedFromDb = recentlyPlayedFromDb,
                onlineSongViewModel = onlineSongViewModel,
                songVm = songViewModel,
                onNavigateToTopSong = onNavigateToTopSong,
                recommendationViewModel = recommendationViewModel,
                isConnected = isConnected,
            )
        }
    }
}

@Composable
fun HomeScreenResponsive(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onlineSongViewModel: OnlineSongViewModel,
    onNavigateToTopSong: (String) -> Unit = {},
    recommendationViewModel: RecommendationViewModel,
    isConnected: Boolean,
    audioOutputViewModel: AudioOutputViewModel
) {
    val configuration = LocalConfiguration.current
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR)
    val context = LocalContext.current
    val newSongsFromDb by songViewModel.newSongs.collectAsState()
    val recentlyPlayedFromDb by songViewModel.recentlyPlayed.collectAsState()
    val appContext = context.applicationContext as? Application
    val api = remember<ApiService> {
        RetrofitClient.create(tokenManager = TokenManager(context))
    }
    val session = remember<SessionManager> { SessionManager(context) }

    val factory = remember<OnlineSongViewModelFactory> { OnlineSongViewModelFactory(api, session, appContext!!) }
    val onlineViewModel: OnlineSongViewModel =
        viewModel(factory = factory)

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                VerticalNavBar(
                    currentRoute = "home",
                    onItemSelected = {
                        when (it) {
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                BottomPlayerSectionFromDB(
                    songViewModel = songViewModel,
                    isPlaying = playerViewModel.isPlaying.collectAsState().value,
                    onPlayPause = { playerViewModel.playPause() },
                    onSectionClick = { }
                )
            }
            HomeScreenWithBottomNav(
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToProfile = onNavigateToProfile,
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
                newSongsFromDb = newSongsFromDb,
                recentlyPlayedFromDb = recentlyPlayedFromDb,
                onlineSongViewModel = onlineSongViewModel,
                songVm = songViewModel,
                onNavigateToTopSong = onNavigateToTopSong,
                isConnected = isConnected,
                audioOutputViewModel = audioOutputViewModel,
                recommendationViewModel = recommendationViewModel
            )
        }
    } else {
        HomeScreenWithBottomNav(
            onNavigateToLibrary = onNavigateToLibrary,
            onNavigateToProfile = onNavigateToProfile,
            songViewModel = songViewModel,
            playerViewModel = playerViewModel,
            newSongsFromDb = newSongsFromDb,
            recentlyPlayedFromDb = recentlyPlayedFromDb,
            onlineSongViewModel = onlineSongViewModel,
            songVm = songViewModel,
            audioOutputViewModel = audioOutputViewModel,
            onNavigateToTopSong = onNavigateToTopSong,
            recommendationViewModel = recommendationViewModel,
            isConnected = isConnected
        )
    }
}