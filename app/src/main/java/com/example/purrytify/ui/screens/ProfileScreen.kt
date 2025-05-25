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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.purrytify.viewmodel.PlayerViewModel
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.text.style.TextOverflow
import java.time.YearMonth
import java.util.Locale
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(
    isConnected: Boolean,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onScanQrClicked: () -> Unit,
    onNavigateToTimeListenedDetail: () -> Unit,
    onNavigateToUserTopSongs: (YearMonth) -> Unit,
    onNavigateToUserTopArtists: (YearMonth) -> Unit
) {
    val context = LocalContext.current

    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val userRepository = remember { UserRepository(AppDatabase.getDatabase(context).userDao()) }
    val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ProfileViewModelFactory(context, tokenManager, userRepository, sessionManager)
    )

    val analytics by profileViewModel.analytics.collectAsState()
    LaunchedEffect(Unit) { profileViewModel.loadMonthlySummaryAnalytics() }

    var showNoInternetDialog by remember { mutableStateOf(!isConnected) }

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
    }

    val uiState = profileViewModel.uiState.value

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
        onEditProfile = onEditProfile,
        songViewModel = songViewModel,
        analytics = analytics,
        playerViewModel = playerViewModel,
        onScanQrCodeClick = onScanQrClicked,
        onNavigateToTimeListenedDetail = onNavigateToTimeListenedDetail,
        onNavigateToUserTopSongs = onNavigateToUserTopSongs,
        onNavigateToUserTopArtists = onNavigateToUserTopArtists,
        profileViewModel = profileViewModel
    )
}

@Composable
fun ProfileContent(
    username: String,
    country: String,
    profilePhotoUrl: String?,
    songsAdded: Int,
    likedSongs: Int,
    listenedSongs: Int,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    songViewModel: SongViewModel,
    analytics: SoundCapsule,
    playerViewModel: PlayerViewModel,
    onScanQrCodeClick: () -> Unit,
    onNavigateToTimeListenedDetail: () -> Unit,
    onNavigateToUserTopSongs: (YearMonth) -> Unit,
    onNavigateToUserTopArtists: (YearMonth) -> Unit,
    profileViewModel: ProfileViewModel
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
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
            Text(
                text = username,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = country,
                style = TextStyle(
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))


            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    backgroundColor = Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Profile",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onLogout()
                    songViewModel.reset()
                },
                modifier = Modifier.fillMaxWidth(0.5f),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = Color.DarkGray.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colors.error
                )
            ) {
                Text(
                    text = "Log Out",
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                    Log.d("ProfileContent", "Scan QR Button CLICKED")
                    onScanQrCodeClick()
                },
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
                songViewModel = songViewModel,
                onNavigateToTimeListenedDetail = onNavigateToTimeListenedDetail,
                onNavigateToUserTopSongs = onNavigateToUserTopSongs,
                onNavigateToUserTopArtists = onNavigateToUserTopArtists,
                profileViewModel = profileViewModel
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
    onEditProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onScanQrClicked: () -> Unit,
    onNavigateToTimeListenedDetail: () -> Unit,
    onNavigateToUserTopSongs: (YearMonth) -> Unit,
    onNavigateToUserTopArtists: (YearMonth) -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "profile",
                onItemSelected = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "library" -> onNavigateToLibrary()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ProfileScreen(
                isConnected = isConnected,
                onLogout = onLogout,
                onEditProfile = onEditProfile,
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
                onScanQrClicked = onScanQrClicked,
                onNavigateToTimeListenedDetail = onNavigateToTimeListenedDetail,
                onNavigateToUserTopSongs = onNavigateToUserTopSongs,
                onNavigateToUserTopArtists = onNavigateToUserTopArtists
            )
        }
    }
}

@Composable
fun SoundCapsuleSection(
    analytics: SoundCapsule,
    onDownload: () -> Unit,
    onShareMonth: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToTimeListenedDetail: () -> Unit,
    onNavigateToUserTopSongs: (YearMonth) -> Unit,
    onNavigateToUserTopArtists: (YearMonth) -> Unit,
    profileViewModel: ProfileViewModel
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val baseMillis = analytics.timeListenedMillis ?: 0L
    var extraMillis by remember { mutableStateOf(0L) }

    val totalTimeListenedMillis = baseMillis + extraMillis
    val formattedTimeListened = run {
        val tot = totalTimeListenedMillis / 1000
        val hours = tot / 3600
        val minutes = (tot % 3600) / 60
        if (hours > 0) {
            String.format(Locale.getDefault(), "%d jam %02d menit", hours, minutes)
        } else {
            String.format(Locale.getDefault(), "%d menit", minutes)
        }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Sound Capsule",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    scope.launch {
                        val csvFile = profileViewModel.prepareAndExportCsv()
                        if (csvFile != null && csvFile.exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    csvFile
                                )
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    type = "text/csv"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Analytics As...")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                Log.e("ProfileScreen", "Error creating share intent for CSV", e)
                                Toast.makeText(context, "Error: Could not share file. ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "No analytics data to export for this month.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Download Analytics",
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = analytics.monthYear.ifEmpty { "Data Belum Tersedia" },
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onShareMonth,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Monthly Recap",
                    tint = Color(0xFFB3B3B3)
                )
            }
        }
        Spacer(Modifier.height(12.dp))


        Card(
            backgroundColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clickable { onNavigateToTimeListenedDetail() }
        ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Time listened",
                        color = Color(0xFFB3B3B3),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formattedTimeListened,
                        color = Color(0xFF1DB954),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = Color(0xFFB3B3B3)
                )
            }
        }

        Spacer(Modifier.height(16.dp))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        analytics.month?.let { month ->
                            onNavigateToUserTopArtists(month)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!analytics.topArtistImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = analytics.topArtistImageUrl,
                            contentDescription = "Top Artist Artwork",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.profile_placeholder)
                        )
                    } else {
                         Icon(painterResource(id = R.drawable.profile_placeholder), contentDescription = "Top Artist Placeholder", modifier = Modifier.size(40.dp).clip(CircleShape), tint = Color.Gray)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top artist",
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        )
                        Text(
                            text = analytics.topArtist ?: "-",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }


            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        analytics.month?.let { month ->
                            onNavigateToUserTopSongs(month)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!analytics.topSongImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = analytics.topSongImageUrl,
                            contentDescription = "Top Song Artwork",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.placeholder_music)
                        )
                    } else {
                        Icon(painterResource(id = R.drawable.placeholder_music), contentDescription = "Top Song Placeholder", modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), tint = Color.Gray)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top song",
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        )
                        Text(
                            text = analytics.topSong ?: "-",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Top Songs", tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp).align(Alignment.CenterVertically))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (analytics.longestDayStreak != null && analytics.longestDayStreak > 0) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    if (!analytics.streakSongArtworkPath.isNullOrEmpty()) {
                        AsyncImage(
                            model = analytics.streakSongArtworkPath,
                            contentDescription = "Streak Song Artwork",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.placeholder_music)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.DarkGray).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "You had a ${analytics.longestDayStreak}-day streak",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = analytics.streakDescriptionText,
                            color = Color(0xFFB3B3B3),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = analytics.streakPeriodText,
                            color = Color(0xFF808080),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
             Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Mulai dengarkan setiap hari untuk membangun streakmu!",
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
