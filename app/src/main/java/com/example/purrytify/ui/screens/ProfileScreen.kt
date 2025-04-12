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
import com.example.purrytify.data.UserRepository
import com.example.purrytify.utils.SessionManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.example.purrytify.ui.navBar.BottomNavBar

@Composable
fun ProfileScreen(
    isConnected: Boolean,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current

    // Membuat TokenManager dari context
    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val userRepository = remember { UserRepository(AppDatabase.getDatabase(context).userDao()) }
    // Mendapatkan ProfileViewModel melalui factory agar dapat menyuntikkan TokenManager
    val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ProfileViewModelFactory(tokenManager, userRepository, sessionManager)
    )

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
        onLogout = onLogout
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
    onLogout: () -> Unit
) {
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
                onClick = onLogout,
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
    onLogout: () -> Unit
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
            ProfileScreen(isConnected = isConnected, onLogout = onLogout)
        }
    }
}