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
import com.example.purrytify.utils.toLocalSong
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

    // Dapatkan ViewModel menggunakan anko-viewModel atau Hilt jika sudah di-setup
    // Untuk sekarang, kita akan coba akses melalui Application Scope atau dengan cara lain jika belum Hilt
    // Cara yang lebih sederhana adalah dengan membuat instance factory di sini jika belum menggunakan Hilt
    private lateinit var onlineSongViewModelInstance: OnlineSongViewModel
    private lateinit var songViewModelInstance: SongViewModel
    private lateinit var playerViewModelInstance: PlayerViewModel
    private lateinit var qrScanLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi ViewModel (ini adalah contoh, sesuaikan dengan setup DI Anda)
        // Jika Anda tidak menggunakan Hilt, Anda perlu membuat factory secara manual di sini.
        // Karena AppNavigation juga membuat instance, ini bisa jadi rumit.
        // Idealnya, ViewModel di-scope ke NavHost atau Activity menggunakan Hilt.

        // Untuk tujuan deep link, kita perlu instance ViewModel yang sama dengan yang digunakan oleh UI.
        // Cara paling mudah adalah dengan membuat factory di sini dan memastikan NavGraph menggunakan ViewModel
        // yang di-scope ke Activity.

        val tokenManager = TokenManager(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        val songRepository = SongRepository(db.songDao(), db.userDao())

        // Factory untuk SongViewModel
        val songViewModelFactory = SongViewModelFactory(songRepository, sessionManager.getUserId().let { if (it == -1) 0 else it })
        songViewModelInstance = ViewModelProvider(this, songViewModelFactory).get(SongViewModel::class.java)

        // Factory untuk PlayerViewModel
        val playerViewModelFactory = PlayerViewModelFactory(application)
        playerViewModelInstance = ViewModelProvider(this, playerViewModelFactory).get(PlayerViewModel::class.java)

        // Factory untuk OnlineSongViewModel
        val apiService = RetrofitClient.create(tokenManager) // Pastikan TokenManager diinisialisasi dengan benar
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
                    onScanQrClicked = { launchQrScanner() } // Ini sudah benar
                )
            }
        }

        // Tangani intent yang masuk saat Activity dibuat
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Tangani intent yang masuk saat Activity sudah berjalan (karena launchMode="singleTop")
        intent?.let {
            handleIntent(it)
        }
    }

    fun launchQrScanner() {
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
                // Tampilkan pesan error ke pengguna
                // Toast.makeText(this, "Invalid Purrytify QR Code", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("DeepLinkHandler", "Error parsing scanned QR data: $scannedData", e)
            // Tampilkan pesan error
        }
    }

    private fun handleIntent(intent: Intent) {
        val action: String? = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            // ... (logika handleIntent yang sudah ada) ...
            // Pastikan menggunakan instance ViewModel yang benar di sini juga
            val scheme = data.scheme
            val host = data.host
            val pathSegments = data.pathSegments

            Log.d("DeepLinkHandler", "Handling Intent - Scheme: $scheme, Host: $host, Segments: $pathSegments")

            if (scheme == "purrytify" && host == "song" && pathSegments.isNotEmpty()) {
                val songIdString = pathSegments.first()
                val songId = songIdString.toIntOrNull()

                if (songId != null) {
                    Log.d("DeepLinkHandler", "Received song ID from deep link: $songId")
                    // Sama seperti di handleScannedQrCode, pastikan ViewModel benar
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
                                // TODO: Navigasi ke Player Screen atau tampilkan BottomSheet Player
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