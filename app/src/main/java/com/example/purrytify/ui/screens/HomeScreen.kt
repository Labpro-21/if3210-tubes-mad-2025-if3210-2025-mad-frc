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
import androidx.compose.material.icons.filled.Download
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
import android.content.res.Configuration
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.network.ApiService
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.utils.TokenManager
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.OnlineSongViewModelFactory
import com.example.purrytify.viewmodel.ProfileViewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Brush


import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import java.util.Date

import com.example.purrytify.ui.components.SongSettingsModal
import com.example.purrytify.ui.components.TopModalBottomSheet

@Composable
fun HomeScreenContent(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    newSongsFromDb: List<Song>,
    recentlyPlayedFromDb: List<Song>,
    onlineViewModel: OnlineSongViewModel,
    songVm: SongViewModel,
) {
    val context = LocalContext.current
    val appContext = if (!LocalInspectionMode.current)
        context.applicationContext as? Application
    else null

    // State untuk modal/dialog
    var showSongSettings by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    // State untuk Top50 Modal Bottom Sheet
    var showTopSheet by remember { mutableStateOf(false) }
    var selectedChartType by remember { mutableStateOf("global") }

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

    val onlineSongs by onlineViewModel.onlineSongs.collectAsState()

    LaunchedEffect(Unit) {
        onlineViewModel.loadTopSongs(null)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Charts Section
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
                // Top 50 Global
                ChartCard(
                    title = "Top 50",
                    subtitle = "GLOBAL",
                    colors = listOf(Color(0xFF0B4870), Color(0xFF16BFFD)),
                    onClick = {
                        selectedChartType = "global"
                        showTopSheet = true}
                )
                
                // Top 50 Indonesia
                ChartCard(
                    title = "Top 10",
                    subtitle = "INDONESIA",
                    colors = listOf(Color(0xFFE34981), Color(0xFFFFB25E)),
                    onClick = {
                        selectedChartType = "ID"
                        showTopSheet = true
                    }
                )
            }
        }

        // New Songs Section
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
                                playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
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

        // Recently Played Section
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
                        playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                    },
                    onMoreClick = {
                        selectedSong = song
                        showSongSettings = true
                    }
                )
            }
        }
    }

    // Modal Settings & Delete Dialog (sama seperti sebelumnya)
    SongSettingsModal(
        visible = showSongSettings,
        onDismiss = { showSongSettings = false },
        onEdit = {
            selectedSong?.let { song ->
                // Implementasi edit logic
            }
        },
        onDelete = {
            showDeleteDialog = true
        },
        onShare = {
            selectedSong?.let { song ->
                shareOnlineSong(context, song)
            }
        },
        isOnlineSong = selectedSong?.audioPath?.startsWith("http") == true
    )

    // Top50 Modal Bottom Sheet - TAMBAHAN BARU
    if (showTopSheet) {
        TopModalBottomSheet(
            visible = showTopSheet,
            chartType = selectedChartType,
            onlineViewModel = onlineViewModel,
            songViewModel = songViewModel,
            playerViewModel = playerViewModel,
            onDismiss = { showTopSheet = false }
        )
    }
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
            .size(120.dp) // Sama dengan ukuran cover lagu
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
        
        // Judul di bawah gambar
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Artist di bawah judul dengan text lebih kecil
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
        // Gambar di paling kiri
        Image(
            painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Judul dan artist di kanan gambar
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Artist di bawah judul
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

// Helper function untuk share online song
private fun shareOnlineSong(context: Context, song: Song) {
    val shareText = "Check out this song: ${song.title} by ${song.artist}\n${song.audioPath}"
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(intent, "Share Song"))
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
    onlineViewModel: OnlineSongViewModel,
    songVm: SongViewModel,
    onNavigateToTop50: (String) -> Unit
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
            song = songViewModel.current_song.collectAsState(initial = null).value ?: return,
            songViewModel = songViewModel,
            onSongChange = { },
            playerViewModel = playerViewModel,
            sheetState = sheetState
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
                onlineViewModel = onlineViewModel,
                songVm = songViewModel,
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
    onNavigateToTop50: (String) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR)
    val context = LocalContext.current
    val appContext = context.applicationContext as? Application

    val newSongsFromDb by songViewModel.newSongs.collectAsState()
    val recentlyPlayedFromDb by songViewModel.recentlyPlayed.collectAsState()

    val api = remember<ApiService> {
        RetrofitClient.create(tokenManager = TokenManager(context))
    }
    val session = remember<SessionManager> { SessionManager(context) }

    val factory = remember<OnlineSongViewModelFactory> { OnlineSongViewModelFactory(api, session) }
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
                onlineViewModel = onlineViewModel,
                songVm = songViewModel,
                onNavigateToTop50 = onNavigateToTop50
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
            onlineViewModel = onlineViewModel,
            songVm = songViewModel,
            onNavigateToTop50 = onNavigateToTop50
        )
    }
}

// helper untuk download + simpan ke DB
private fun downloadSong(
    context: Context,
    networkSong: Song,
    songVm: SongViewModel
) {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val req = DownloadManager.Request(Uri.parse(networkSong.audioPath))
        .setTitle(networkSong.title)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(
            context, Environment.DIRECTORY_MUSIC, "${networkSong.title}.mp3"
        )
    val id = dm.enqueue(req)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == id) {
                val q = DownloadManager.Query().setFilterById(id)
                dm.query(q).use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (idx >= 0) {
                            val uri = c.getString(idx)
                            val offline = networkSong.copy(
                                id = 0,
                                audioPath = uri,
                                addedDate = Date(),
                                lastPlayed = null
                            )
                            songVm.addSong(offline)
                        }
                    }
                }
                ctx.unregisterReceiver(this)
            }
        }
    }
    context.registerReceiver(
        receiver,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        Context.RECEIVER_NOT_EXPORTED
    )
}