package com.example.purrytify.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.purrytify.R
import com.example.purrytify.utils.MapPicker
import com.example.purrytify.viewmodel.ProfileViewModel
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.IOException
import java.util.Locale


private var countryNameToCodeCache: Map<String, String>? = null
private var countryCodeToDisplayNameCache: Map<String, String>? = null

fun getCountryNameToCodeMap(): Map<String, String> {
    if (countryNameToCodeCache == null) {
        val mutableMap = mutableMapOf<String, String>()
        val isoCountryCodes = Locale.getISOCountries()
        val localesToConsider = listOf(
            Locale.getDefault(), Locale.ENGLISH, Locale.US, Locale.UK, Locale("id", "ID")
        )
        for (code in isoCountryCodes) {
            for (locale in localesToConsider) {
                val countryName = Locale("", code).getDisplayCountry(locale)
                if (countryName.isNotBlank()) {
                    mutableMap.putIfAbsent(countryName.lowercase(locale), code)
                    val genericCountryName = Locale("", code).getDisplayCountry(Locale.ENGLISH)
                    if (genericCountryName.isNotBlank() && genericCountryName.lowercase(Locale.ENGLISH) != countryName.lowercase(locale)) {
                        mutableMap.putIfAbsent(genericCountryName.lowercase(Locale.ENGLISH), code)
                    }
                }
            }
            mutableMap.putIfAbsent(code.lowercase(Locale.getDefault()), code)
        }
        countryNameToCodeCache = mutableMap
        Log.d("CountryUtils", "Country name to code map initialized with ${mutableMap.size} entries.")
    }
    return countryNameToCodeCache!!
}

fun getCountryCodeToDisplayNameMap(defaultLocale: Locale = Locale.getDefault()): Map<String, String> {
    if (countryCodeToDisplayNameCache == null || countryCodeToDisplayNameCache?.get(Locale.getISOCountries().firstOrNull() ?: "US") != Locale("", Locale.getISOCountries().firstOrNull() ?: "US").getDisplayCountry(defaultLocale)) {
        val mutableMap = mutableMapOf<String, String>()
        val isoCountryCodes = Locale.getISOCountries()
        for (code in isoCountryCodes) {
            val displayName = Locale("", code).getDisplayCountry(defaultLocale)
            if (displayName.isNotBlank()) {
                mutableMap[code] = displayName
            }
        }
        countryCodeToDisplayNameCache = mutableMap
        Log.d("CountryUtils", "Country code to display name map initialized/updated for locale: ${defaultLocale.toLanguageTag()}")
    }
    return countryCodeToDisplayNameCache!!
}

fun getDisplayCountryNameFromAlpha2Code(alpha2Code: String, defaultLocale: Locale = Locale.getDefault()): String {
    if (alpha2Code.isBlank()) return "Not Set"
    return getCountryCodeToDisplayNameMap(defaultLocale)[alpha2Code] ?: alpha2Code
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by profileViewModel.uiState
    val context = LocalContext.current

    var selectedCountryCode by remember(uiState.country) {
        mutableStateOf(
            if (uiState.country.matches(Regex("^[A-Z]{2}$"))) uiState.country else ""
        )
    }
    val displayCountryName by remember(selectedCountryCode) {
        derivedStateOf {
            if (selectedCountryCode.isNotBlank()) {
                getDisplayCountryNameFromAlpha2Code(selectedCountryCode, Locale.getDefault())
            } else {
                "Not Set"
            }
        }
    }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val currentPhotoUrl by remember(uiState.profilePhoto) { mutableStateOf(uiState.profilePhoto) }
    var showMapDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        getCountryNameToCodeMap()
        getCountryCodeToDisplayNameMap(Locale.getDefault())
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestCurrentLocation(context) { countryCode ->
                countryCode?.let { selectedCountryCode = it }
            }
        } else {
            Log.w("EditProfileScreen", "Location permission denied by user.")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> selectedPhotoUri = uri }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val tempFile = File(context.cacheDir, "temp_profile_pic_${System.currentTimeMillis()}.jpg")
            try {
                tempFile.outputStream().use { out -> it.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                selectedPhotoUri = tempFile.toUri()
            } catch (e: Exception) { Log.e("EditProfileScreen", "Error saving camera bitmap", e) }
        }
    }

    if (showMapDialog) {
        Dialog(onDismissRequest = { showMapDialog = false }) {
            Surface(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, tonalElevation = AlertDialogDefaults.TonalElevation) {
                MapPicker(modifier = Modifier.fillMaxSize(), onLocationPicked = { countryCode ->
                    selectedCountryCode = countryCode
                    showMapDialog = false
                })
            }
        }
    }

    if (showPhotoSourceDialog) {
        PhotoSourcePickerDialog(
            onDismiss = { showPhotoSourceDialog = false },
            onGalleryClick = { galleryLauncher.launch("image/*"); showPhotoSourceDialog = false },
            onCameraClick = { cameraLauncher.launch(null); showPhotoSourceDialog = false }
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF006175), Color(0xFF121212))
                )
            )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Edit Profile", fontWeight = FontWeight.SemiBold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                        modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showPhotoSourceDialog = true }) {
                    Text("Change Photo", color = Color.White.copy(alpha = 0.8f))
                }

                Spacer(Modifier.height(32.dp))

                SectionTitle("Location", color = Color.White.copy(alpha = 0.9f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location Icon",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = displayCountryName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
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
                                        countryCode?.let { selectedCountryCode = it }
                                    }
                                }
                                else -> { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Use Current Location", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Current")
                    }

                    Button(
                        onClick = { showMapDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.EditLocation, contentDescription = "Pick on Map", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("On Map")
                    }
                }

                Spacer(Modifier.weight(1f))

                profileViewModel.errorMsg?.let { serverError ->
                    Text(
                        text = serverError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))


                Button(
                    onClick = {
                        profileViewModel.clearErrorMsg()
                        val locationToSave = if (selectedCountryCode.isNotBlank()) selectedCountryCode else null
                        Log.d("EditProfileScreen", "Saving profile. Location Code: $locationToSave, Photo URI: $selectedPhotoUri")
                        profileViewModel.updateProfile(location = locationToSave, photoUri = selectedPhotoUri) { success ->
                            if (success) onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !profileViewModel.isLoading,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    if (profileViewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Save Changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


@Composable
fun SectionTitle(title: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
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
        text = { Column { Text("Choose a source for your new profile photo.") } },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@SuppressLint("MissingPermission")
fun requestCurrentLocation(context: Context, onResult: (String?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                val countryCode = addresses.firstOrNull()?.countryCode
                                Log.d("EditProfileScreen", "Current Location (Android 33+) - Country Code: $countryCode, Lat: ${location.latitude}, Lng: ${location.longitude}")
                                onResult(countryCode)
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val countryCode = addresses?.firstOrNull()?.countryCode
                            Log.d("EditProfileScreen", "Current Location (Pre-Android 33) - Country Code: $countryCode, Lat: ${location.latitude}, Lng: ${location.longitude}")
                            onResult(countryCode)
                        }
                    } catch (e: IOException) {
                        Log.e("EditProfileScreen", "Geocoder IOException: ${e.message}", e)
                        onResult(null)
                    } catch (e: Exception) {
                        Log.e("EditProfileScreen", "Geocoder general error", e)
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
    } catch (se: SecurityException) {
        Log.e("EditProfileScreen", "SecurityException in requestCurrentLocation. Is permission granted?", se)
        onResult(null)
    }
}