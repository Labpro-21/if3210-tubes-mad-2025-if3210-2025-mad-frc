package com.example.purrytify.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.purrytify.R
import com.example.purrytify.viewmodel.ProfileViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.example.purrytify.ui.navBar.BottomNavBar

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel()

    // Ambil token yang disimpan dan dapatkan profile dari API
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("accessToken", null)
        token?.let {
            profileViewModel.fetchUserProfile(it)
        }
    }

    val uiState = profileViewModel.uiState.value

    ProfileScreen(
        username = uiState.username,
        email = uiState.email,
        // Jika API mengembalikan nama file foto, buat URL dengan format yang sesuai
        profilePhotoUrl = if (uiState.profilePhoto.isNotEmpty()) {
            "http://34.101.226.132:3000/uploads/profile-picture/${uiState.profilePhoto}"
        } else {
            null
        },
        songsAdded = uiState.songsAdded,
        likedSongs = uiState.likedSongs,
        listenedSongs = uiState.listenedSongs,
        onBack = onBack
    )
}

@Composable
fun ProfileScreen(
    username: String,
    email: String,
    profilePhotoUrl: String?,
    songsAdded: Int,
    likedSongs: Int,
    listenedSongs: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header: tampilkan foto profil dan data pengguna
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!profilePhotoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = profilePhotoUrl,
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.profile_placeholder),
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = username, style = MaterialTheme.typography.h5)
                Text(text = email, style = MaterialTheme.typography.body1)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$songsAdded", style = MaterialTheme.typography.h6)
                Text(text = "Songs Added", style = MaterialTheme.typography.body2)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$likedSongs", style = MaterialTheme.typography.h6)
                Text(text = "Liked Songs", style = MaterialTheme.typography.body2)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$listenedSongs", style = MaterialTheme.typography.h6)
                Text(text = "Listened Songs", style = MaterialTheme.typography.body2)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Back")
        }
    }
}

@Composable
fun ProfileScreenWithBottomNav(
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onBack: () -> Unit
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
            ProfileScreen(onBack = onBack)
        }
    }
}