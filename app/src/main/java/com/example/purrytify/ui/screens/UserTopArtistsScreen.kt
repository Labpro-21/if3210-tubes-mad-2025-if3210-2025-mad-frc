package com.example.purrytify.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.R
import com.example.purrytify.data.ArtistRankInfo
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.ProfileViewModel
import com.example.purrytify.viewmodel.SongViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTopArtistsScreen(
    profileViewModel: ProfileViewModel,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    audioOutputViewModel: AudioOutputViewModel,
    yearMonthString: String, // "YYYY-MM"
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    // Tambahkan onArtistClick jika ingin ada aksi saat artis di-klik
    // onArtistClick: (ArtistRankInfo) -> Unit 
) {
    val topPlayedArtists by profileViewModel.userTopPlayedArtists.collectAsState()
    val totalDistinctArtists by profileViewModel.totalDistinctArtists.collectAsState()
    val currentGlobalSong by songViewModel.current_song.collectAsState()
    val isGlobalPlaying by playerViewModel.isPlaying.collectAsState()
    var showPlayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val yearMonth = remember(yearMonthString) { YearMonth.parse(yearMonthString) }
    val monthDisplay = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    LaunchedEffect(yearMonth) {
        profileViewModel.loadUserTopPlayedArtists(yearMonth)
    }

    if (showPlayerSheet && currentGlobalSong != null) {
        PlayerModalBottomSheet(
            showSheet = showPlayerSheet,
            onDismiss = { showPlayerSheet = false },
            song = currentGlobalSong!!,
            songViewModel = songViewModel,
            onSongChange = { /* Logika play next/prev jika relevan untuk player global */ },
            playerViewModel = playerViewModel,
            sheetState = sheetState,
            audioOutputViewModel = audioOutputViewModel
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top artists") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column {
                if (currentGlobalSong != null) {
                    BottomPlayerSectionFromDB(
                        songViewModel = songViewModel,
                        isPlaying = isGlobalPlaying,
                        onPlayPause = { playerViewModel.playPause() },
                        onSectionClick = { showPlayerSheet = true }
                    )
                }
                BottomNavBar(
                    currentRoute = "profile_top_artists", // Rute unik
                    onItemSelected = { route ->
                        when (route) {
                            "home" -> onNavigateToHome()
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), // Tambah padding bawah
            ) {
                Text(
                    text = monthDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You listened to ${totalDistinctArtists} artists this month.",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            if (topPlayedArtists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No significant artist data for this month.",
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Jarak antar item
                ) {
                    itemsIndexed(topPlayedArtists) { index, artistInfo ->
                        TopArtistListItem(
                            rank = index + 1,
                            artistInfo = artistInfo,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopArtistListItem(
    rank: Int,
    artistInfo: ArtistRankInfo,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)) // Opsional: jika ingin background item berbeda
            // .background(Color.DarkGray.copy(alpha = 0.2f)) // Contoh background
            .padding(vertical = 8.dp), // Padding vertikal untuk setiap item
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%02d".format(rank),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White, // Atau MaterialTheme.colorScheme.primary jika ingin menonjol
                fontWeight = FontWeight.Medium // Sedikit lebih tipis dari bold
            ),
            modifier = Modifier.width(32.dp) // Lebar tetap untuk rank
        )

        Spacer(modifier = Modifier.width(16.dp))

        AsyncImage(
            model = artistInfo.artworkPath?.toUri(),
            contentDescription = "Artwork for ${artistInfo.artistName}",
            modifier = Modifier
                .size(56.dp) // Ukuran gambar artis
                .clip(CircleShape), // Bentuk lingkaran untuk gambar artis
            contentScale = ContentScale.Crop,
            error = rememberAsyncImagePainter(model = R.drawable.profile_placeholder) // Placeholder jika gambar gagal dimuat atau null
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = artistInfo.artistName,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), // Ukuran font disesuaikan
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Jika ingin menambahkan ikon panah atau aksi lain di ujung kanan
        // Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, ...)
    }
}