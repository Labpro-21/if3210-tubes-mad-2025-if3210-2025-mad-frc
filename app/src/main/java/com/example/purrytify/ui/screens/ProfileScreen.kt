package com.example.purrytify.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.purrytify.R
import com.example.purrytify.viewmodel.ProfileViewModel
import com.example.purrytify.viewmodel.ProfileViewModelFactory
import com.example.purrytify.utils.TokenManager
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.utils.SessionManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.ui.LockScreenOrientation
import android.content.pm.ActivityInfo
import androidx.compose.material.Card
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.model.SoundCapsule
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.purrytify.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.filled.QrCodeScanner

@Composable
fun ProfileScreen(
    isConnected: Boolean,
    onLogout: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onScanQrClicked: () -> Unit
) {
    val context = LocalContext.current

    // Membuat TokenManager dari context
    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val userRepository = remember { UserRepository(AppDatabase.getDatabase(context).userDao()) }
    // Mendapatkan ProfileViewModel melalui factory agar dapat menyuntikkan TokenManager
    val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ProfileViewModelFactory(context, tokenManager, userRepository, sessionManager)
    )

    val analytics by profileViewModel.analytics.collectAsState()
    LaunchedEffect(Unit) { profileViewModel.loadAnalytics() }

    var showNoInternetDialog by remember { mutableStateOf(!isConnected) }

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    // Panggil API profile ketika komposisi pertama kali dijalankan
    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
    }

    val uiState = profileViewModel.uiState.value

    // Karena data negara tidak tersedia dalam uiState, gunakan contoh default ("Indonesia")
    ProfileContent(
        username = uiState.username,
        country = uiState.country,
        profilePhotoUrl = if (uiState.profilePhoto.isNotBlank())
            "http://34.101.226.132:3000/uploads/profile-picture/${uiState.profilePhoto}"
        else null,
        songsAdded = uiState.songsAdded,
        likedSongs = uiState.likedSongs,
        listenedSongs = uiState.listenedSongs,
        onLogout = onLogout,
        songViewModel = songViewModel,
        analytics = analytics,
        playerViewModel = playerViewModel,
        onScanQrCodeClick = onScanQrClicked // Tambahkan parameter untuk fungsi scan QR
    )
}

@Composable
fun ProfileContent(
    username: String,
    country: String, // negara asal
    profilePhotoUrl: String?,
    songsAdded: Int,
    likedSongs: Int,
    listenedSongs: Int,
    onLogout: () -> Unit,
    songViewModel: SongViewModel,
    analytics: SoundCapsule,
    playerViewModel: PlayerViewModel,
    onScanQrCodeClick: () -> Unit // Tambahkan parameter untuk fungsi scan QR
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    // Background gradasi dari #006175 ke #121212
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF006175), Color(0xFF121212))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            // Foto profil ditempatkan di tengah, tidak terlalu di atas
            if (!profilePhotoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = profilePhotoUrl,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.profile_placeholder),
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Username dengan align center
            Text(
                text = username,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp
                    // Tambahkan fontFamily Poppins jika tersedia
                ),
                textAlign = TextAlign.Center
            )
            // Teks negara dengan ukuran lebih kecil dan berwarna #B3B3B3
            Text(
                text = country,
                style = TextStyle(
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Tombol Log Out dengan teks putih dan background #3E3F3F
            Button(
                onClick = {
                    onLogout()
                    songViewModel.reset()
                },
                modifier = Modifier.fillMaxWidth(0.5f),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3E3F3F)
                )
            ) {
                Text(text = "Log Out", color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
            // Statistik: Songs, Liked, Listened - sejajar secara horizontal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = songsAdded, label = "SONGS")
                StatItem(count = likedSongs, label = "LIKED")
                StatItem(count = listenedSongs, label = "LISTENED")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Panggil fungsi untuk memulai pemindaian QR
                    // Kita akan menggunakan ActivityResultLauncher untuk ini
//                     onScanQrCodeClick() -> ini akan jadi parameter ke ProfileScreen
                },
                // ... modifier dan style lainnya ...
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR Code", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Scan Song QR")
            }

            SoundCapsuleSection(
                analytics = analytics,
                onDownload = { /* export CSV / PDF */ },
                onShareMonth = { /* share summary bulan ini */ },
                playerViewModel = playerViewModel,
                songViewModel = songViewModel
            )
        }
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            )
        )
    }
}

@Composable
fun ProfileScreenWithBottomNav(
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    isConnected: Boolean,
    onLogout: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onScanQrClicked: () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "profile",
                onItemSelected = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "library" -> onNavigateToLibrary()
                        // Jika route "profile" dipilih, berarti saat ini sedang aktif.
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ProfileScreen(isConnected = isConnected, onLogout = onLogout, songViewModel = songViewModel, playerViewModel = playerViewModel, onScanQrClicked = onScanQrClicked)
        }
    }
}

@Composable
fun SoundCapsuleSection(
    analytics: SoundCapsule,
    onDownload: () -> Unit,
    onShareMonth: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    val baseMillis = analytics.timeListenedMillis ?: 0L
    var extraMillis by remember { mutableStateOf(0L) }
//    print information to logcat tentang analytics
    Log.d("SoundCapsuleSection", "analytics: $analytics")

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1_000L)
            Log.d("SoundCapsuleSection", "isActive=$isActive, isPlaying=$isPlaying, extra=$extraMillis")
            if (isPlaying) {
                extraMillis += 1_000L
                songViewModel.recordPlayTick()
            }
        }
    }

    val total = baseMillis + extraMillis
    val formatted = run {
        val sec = total / 1_000
        "%d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header: "Your Sound Capsule" + download icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Sound Capsule",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Download",
                    tint = Color.White
                )
            }
        }

        // Month header: e.g. "April 2025" + share icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = analytics.monthYear ?: "",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onShareMonth,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Month",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Time listened card
        Card(
            backgroundColor = Color(0xFF1F1F1F),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
        ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time listened",
                        color = Color(0xFFB3B3B3),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$formatted minutes",
                        color = Color(0xFF3CDC76),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Row: Top artist & Top song
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Artist
            Card(
                backgroundColor = Color(0xFF1F1F1F),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = analytics.topArtistImageUrl,
                        contentDescription = "Artist",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top artist",
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        )
                        Text(
                            text = analytics.topArtist ?: "-",
                            color = Color(0xFF3884FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // Top Song
            Card(
                backgroundColor = Color(0xFF1F1F1F),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = analytics.topSongImageUrl,
                        contentDescription = "Song",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top song",
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        )
                        Text(
                            text = analytics.topSong ?: "-",
                            color = Color(0xFFFFE400),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Streak card with cover image
        Card(
            backgroundColor = Color(0xFF1F1F1F),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                AsyncImage(
                    model = analytics.streakCoverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "You had a ${analytics.dayStreak}-day streak",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = analytics.streakDescription ?: "",
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 8.dp)
                )
                Text(
                    text = analytics.streakPeriod ?: "",
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 16.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { /* share streak */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share streak",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

//@Composable
//fun IconButton(onClick: () -> Unit, content: @Composable () -> Icon) {
//    TODO("Not yet implemented")
//}
