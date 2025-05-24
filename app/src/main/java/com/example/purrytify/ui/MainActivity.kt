package com.example.purrytify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Untuk by viewModels()
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.viewmodel.OnlineSongViewModelFactory
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.result.ActivityResultLauncher

// Import untuk ZXing Scanner
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions


class MainActivity : ComponentActivity() {

    private lateinit var onlineSongViewModelInstance: OnlineSongViewModel
    private lateinit var playerViewModelInstance: PlayerViewModel
    private lateinit var qrScanLauncher: ActivityResultLauncher<ScanOptions>

    val songViewModel: SongViewModel by viewModels {
        val sm = SessionManager(applicationContext)
        val sId = sm.getUserId().let { if (it == -1) 0 else it } // Hati-hati dengan ID 0 jika tidak valid
        Log.d("MainActivity_VM", "Creating SongViewModel in MainActivity for userId: $sId")
        SongViewModelFactory(
            SongRepository(AppDatabase.getDatabase(applicationContext).songDao(), AppDatabase.getDatabase(applicationContext).userDao()),
            sId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        val songRepository = SongRepository(db.songDao(), db.userDao())

        // Factory untuk PlayerViewModel
        val playerViewModelFactory = PlayerViewModelFactory(application)
        playerViewModelInstance = ViewModelProvider(this, playerViewModelFactory).get(PlayerViewModel::class.java)

        // Factory untuk OnlineSongViewModel
        val apiService = RetrofitClient.create(tokenManager)
        val onlineSongViewModelFactory = OnlineSongViewModelFactory(apiService, sessionManager)
        onlineSongViewModelInstance = ViewModelProvider(this, onlineSongViewModelFactory).get(OnlineSongViewModel::class.java)

        qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Log.d("MainActivity", "QR Scan cancelled")
            } else {
                Log.d("MainActivity", "QR Scan successful: ${result.contents}")
                handleScannedQrCode(result.contents)
            }
        }

        setContent {
            PurrytifyTheme {
                AppNavigation(
                    songViewModelFromActivity = songViewModel,
                    onScanQrClicked = { launchQrScanner() } // Ini sudah benar
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleIntent(it)
        }
    }

    fun launchQrScanner() {
        Log.d("MainActivity_QR", "launchQrScanner() CALLED")
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a Purrytify Song QR Code")
        options.setCameraId(0)  // Gunakan kamera belakang
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        qrScanLauncher.launch(options)
    }

    private fun handleScannedQrCode(scannedData: String) {
        Log.d("DeepLinkHandler", "Scanned QR Data: $scannedData")
        try {
            val scannedUri = Uri.parse(scannedData)
            if (scannedUri.scheme == "purrytify" && scannedUri.host == "song") {
                // Ini adalah deep link Purrytify, proses seperti deep link biasa
                val intent = Intent(Intent.ACTION_VIEW, scannedUri)
                handleIntent(intent) // Gunakan fungsi handleIntent yang sudah ada
            } else {
                Log.w("DeepLinkHandler", "Scanned QR is not a valid Purrytify song link: $scannedData")
            }
        } catch (e: Exception) {
            Log.e("DeepLinkHandler", "Error parsing scanned QR data: $scannedData", e)
        }
    }

    private fun handleIntent(intent: Intent) {
        val action: String? = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            val scheme = data.scheme
            val host = data.host
            val pathSegments = data.pathSegments

            Log.d("DeepLinkHandler", "Handling Intent - Scheme: $scheme, Host: $host, Segments: $pathSegments")

            if (scheme == "purrytify" && host == "song" && pathSegments.isNotEmpty()) {
                val songIdString = pathSegments.first()
                val songId = songIdString.toIntOrNull()

                if (songId != null) {
                    Log.d("DeepLinkHandler", "Received song ID from deep link: $songId")
                    val tokenManager = TokenManager(applicationContext)
                    val sessionManager = SessionManager(applicationContext)
                    val apiService = RetrofitClient.create(tokenManager)
                    val localOnlineSongViewModel = ViewModelProvider(this, OnlineSongViewModelFactory(apiService, sessionManager)).get(OnlineSongViewModel::class.java)

                    val db = AppDatabase.getDatabase(applicationContext)
                    val songRepository = SongRepository(db.songDao(), db.userDao())
                    val localSongViewModel = ViewModelProvider(this, SongViewModelFactory(songRepository, sessionManager.getUserId().let { if(it == -1) 0 else it})).get(SongViewModel::class.java)
                    val localPlayerViewModel = ViewModelProvider(this, PlayerViewModelFactory(application)).get(PlayerViewModel::class.java)

                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val response = localOnlineSongViewModel.fetchSongById(songId)
                            if (response != null) {
                                Log.i("DeepLinkHandler", "Song fetched via deep link: ${response.title}")
                                localSongViewModel.setCurrentSong(response)
                                localPlayerViewModel.prepareAndPlay(response.audioPath.toUri()) {}
                            } else {
                                Log.e("DeepLinkHandler", "Failed to fetch song with ID: $songId from server via deep link.")
                            }
                        } catch (e: Exception) {
                            Log.e("DeepLinkHandler", "Error fetching song by ID $songId via deep link: ${e.message}", e)
                        }
                    }
                } else {
                    Log.e("DeepLinkHandler", "Invalid song ID format in deep link: $songIdString")
                }
            }
        }
    }
}