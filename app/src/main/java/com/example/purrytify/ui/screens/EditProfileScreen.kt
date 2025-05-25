package com.example.purrytify.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.util.Log // Pastikan Log diimpor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.purrytify.R // Pastikan Anda punya placeholder drawable
import com.example.purrytify.utils.MapPicker // Asumsi MapPicker Anda sudah baik
import com.example.purrytify.viewmodel.ProfileViewModel
import com.google.android.gms.location.LocationServices
import java.io.File
import java.util.Locale

// Fungsi validasi kode negara (bisa ditaruh di luar Composable atau di file utilitas)
fun isValidCountryCode(code: String): Boolean {
    // Kode negara ISO 3166-1 alpha-2 biasanya 2 huruf, alpha-3 itu 3 huruf.
    // Kita akan validasi untuk 2 huruf kapital. Anda bisa sesuaikan.
    // Regex ini mengecek apakah string terdiri dari tepat 2 huruf alfabet.
    return code.matches(Regex("^[A-Z]{2}$"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by profileViewModel.uiState
    var displayLocation by remember(uiState.country) { mutableStateOf(uiState.country) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val currentPhotoUrl by remember(uiState.profilePhoto) { mutableStateOf(uiState.profilePhoto) }

    var showMapDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // State baru untuk pesan error validasi input lokasi
    var locationInputError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestCurrentLocation(context) { countryCode ->
                countryCode?.let {
                    displayLocation = it
                    locationInputError = null // Hapus error jika lokasi diupdate dari GPS
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val tempFile = File(context.cacheDir, "temp_profile_pic_${System.currentTimeMillis()}.jpg")
            try {
                tempFile.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                selectedPhotoUri = tempFile.toUri()
            } catch (e: Exception) {
                Log.e("EditProfileScreen", "Error saving camera bitmap", e)
            }
        }
    }

    if (showMapDialog) {
        Dialog(onDismissRequest = { showMapDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                MapPicker(
                    modifier = Modifier.fillMaxSize(),
                    onLocationPicked = { countryCode ->
                        displayLocation = countryCode
                        locationInputError = null // Hapus error jika lokasi diupdate dari Map
                        showMapDialog = false
                    }
                )
            }
        }
    }

    if (showPhotoSourceDialog) {
        PhotoSourcePickerDialog(
            onDismiss = { showPhotoSourceDialog = false },
            onGalleryClick = {
                galleryLauncher.launch("image/*")
                showPhotoSourceDialog = false
            },
            onCameraClick = {
                cameraLauncher.launch(null)
                showPhotoSourceDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showPhotoSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedPhotoUri ?: if (currentPhotoUrl.isNotEmpty()) {
                        if (currentPhotoUrl.startsWith("http")) currentPhotoUrl else "http://34.101.226.132:3000/uploads/profile-picture/$currentPhotoUrl"
                    } else {
                        R.drawable.profile_placeholder
                    },
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.profile_placeholder),
                    error = painterResource(id = R.drawable.profile_placeholder)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showPhotoSourceDialog = true }) {
                Text("Change Photo")
            }

            Spacer(Modifier.height(32.dp))

            SectionTitle("Location")
            OutlinedTextField(
                value = displayLocation,
                onValueChange = {
                    displayLocation = it.uppercase() // Otomatis uppercase
                    // Hapus error saat pengguna mulai mengetik lagi
                    if (locationInputError != null) {
                        locationInputError = null
                    }
                },
                label = { Text("Country Code (e.g., ID, US)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = locationInputError != null, // Tampilkan error jika ada
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    // Warna error default sudah cukup baik, tapi bisa dikustomisasi
                    // errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            // Tampilkan pesan error validasi di bawah TextField
            locationInputError?.let { errorText ->
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                requestCurrentLocation(context) { countryCode ->
                                    countryCode?.let {
                                        displayLocation = it
                                        locationInputError = null // Hapus error
                                    }
                                }
                            }
                            else -> {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use Current Location", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Current")
                }
                OutlinedButton(
                    onClick = { showMapDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.EditLocation, contentDescription = "Pick on Map", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("On Map")
                }
            }

            Spacer(Modifier.weight(1f))

            // Error dari ViewModel (misalnya error server)
            profileViewModel.errorMsg?.let { serverError ->
                Text(
                    text = serverError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    // Hapus error ViewModel lama sebelum mencoba lagi
                    profileViewModel.clearErrorMsg() // Anda perlu menambahkan fungsi ini di ViewModel

                    // Validasi input kode negara
                    val trimmedLocation = displayLocation.trim()
                    if (trimmedLocation.isEmpty() || isValidCountryCode(trimmedLocation)) {
                        locationInputError = null // Tidak ada error atau input valid
                        profileViewModel.updateProfile(
                            location = if (trimmedLocation.isEmpty()) null else trimmedLocation, // Kirim null jika kosong
                            photoUri = selectedPhotoUri
                        ) { success ->
                            if (success) {
                                onBack()
                            }
                            // Pesan error dari ViewModel akan ditampilkan jika 'success' adalah false
                        }
                    } else {
                        // Set pesan error validasi
                        locationInputError = "Invalid country code format. Use 2 letters (e.g., ID, US)."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !profileViewModel.isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (profileViewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// (SectionTitle, PhotoSourcePickerDialog, requestCurrentLocation tetap sama seperti sebelumnya)
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
fun PhotoSourcePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Profile Photo") },
        text = {
            Column {
                Text("Choose a source for your new profile photo.")
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onGalleryClick) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.padding(end = 4.dp))
                    Text("Gallery")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onCameraClick) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", modifier = Modifier.padding(end = 4.dp))
                    Text("Camera")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@SuppressLint("MissingPermission")
fun requestCurrentLocation(context: android.content.Context, onResult: (String?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            val countryCode = addresses.firstOrNull()?.countryCode
                            Log.d("EditProfileScreen", "Current Location - Country Code: $countryCode")
                            onResult(countryCode)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val countryCode = addresses?.firstOrNull()?.countryCode
                        Log.d("EditProfileScreen", "Current Location - Country Code: $countryCode")
                        onResult(countryCode)
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileScreen", "Geocoder error", e)
                    onResult(null)
                }
            } else {
                Log.w("EditProfileScreen", "Fused location is null.")
                onResult(null)
            }
        }
        .addOnFailureListener { e ->
            Log.e("EditProfileScreen", "Error getting fused location", e)
            onResult(null)
        }
}