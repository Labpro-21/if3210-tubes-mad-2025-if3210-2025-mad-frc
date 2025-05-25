package com.example.purrytify.ui.screens

import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.purrytify.utils.MapPicker
import com.example.purrytify.viewmodel.ProfileViewModel
import com.google.android.gms.location.LocationServices
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import java.io.File
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val ui by profileViewModel.uiState
    var loc by remember { mutableStateOf(ui.country) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var showMap by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // runtime permission launcher untuk lokasi
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* nothing */ }

    // pick from gallery
    val galLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> photo = uri }

    // take picture from camera
    val camLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let {
            val tmp = File(ctx.cacheDir, "profile_tmp.jpg")
            tmp.outputStream().use { out -> it.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            photo = tmp.toUri()
        }
    }

    // fused location client
    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }

    // dialog untuk MapPicker
    if (showMap) {
        Dialog(onDismissRequest = { showMap = false }) {
            MapPicker(
                modifier = Modifier.fillMaxSize(),
                onLocationPicked = { cc ->
                    loc = cc
                    showMap = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // === Location Section ===
            Text("Location", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = loc,
                onValueChange = { loc = it },
                label = { Text("Country code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        // cek permission lalu ambil lastLocation
                        if (ContextCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            fused.lastLocation.addOnSuccessListener { locn ->
                                locn?.let {
                                    val geo = Geocoder(ctx, Locale.getDefault())
                                    geo.getFromLocation(it.latitude, it.longitude, 1)
                                        ?.firstOrNull()?.countryCode
                                        ?.let { cc -> loc = cc }
                                }
                            }
                        } else {
                            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                ) {
                    Text("Use Current")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { showMap = true }
                ) {
                    Text("Pick on Map")
                }
            }

            Spacer(Modifier.height(24.dp))

            // === Photo Section ===
            Text("Profile Photo", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { galLauncher.launch("image/*") }) {
                    Text("Gallery")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { camLauncher.launch(null) }) {
                    Text("Camera")
                }
            }
            photo?.let {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = it,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(32.dp))

            // === Save Button ===
            Button(
                onClick = {
                    profileViewModel.updateProfile(loc, photo) { success ->
                        if (success) onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (profileViewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
                }
            }

            // error message
            profileViewModel.errorMsg?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = Color.Red)
            }
        }
    }
}