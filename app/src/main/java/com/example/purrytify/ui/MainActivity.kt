package com.example.purrytify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels // Import untuk by viewModels
import androidx.lifecycle.ViewModelProvider // Bisa juga pakai ini
import androidx.core.net.toUri
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

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {

    // Gunakan by viewModels untuk cara yang lebih bersih dan direkomendasikan
    private val songViewModel: SongViewModel by viewModels {
        val sm = SessionManager(applicationContext)
        // Penting: userId harus valid saat ViewModel dibuat atau bisa menyebabkan masalah FK
        // Kita akan mengandalkan AppNavigation untuk menangani logika jika user belum login.
        // Untuk instance di Activity, kita bisa gunakan userId saat ini,
        // AppNavigation akan membuat ulang VM dengan key baru jika userId berubah.
        val userIdForFactory = sm.getUserId().let { if (it <= 0) 0 else it } // Default ke 0 jika tidak valid
        Log.d("MainActivity_VM", "MainActivity creating SongViewModel with factory for userId: $userIdForFactory")
        SongViewModelFactory(
            SongRepository(AppDatabase.getDatabase(applicationContext).songDao(), AppDatabase.getDatabase(applicationContext).userDao()),
            userIdForFactory
        )
    }
    private val playerViewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(application)
    }
    private val onlineSongViewModel: OnlineSongViewModel by viewModels {
        val tm = TokenManager(applicationContext)
        val sm = SessionManager(applicationContext)
        OnlineSongViewModelFactory(RetrofitClient.create(tm), sm)
    }

    private lateinit var qrScanLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("ViewModelInstance", "MainActivity - SongViewModel hash: ${System.identityHashCode(songViewModel)}")
        Log.d("ViewModelInstance", "MainActivity - PlayerViewModel hash: ${System.identityHashCode(playerViewModel)}")
        Log.d("ViewModelInstance", "MainActivity - OnlineSongViewModel hash: ${System.identityHashCode(onlineSongViewModel)}")


        qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Log.d("MainActivity_QR", "QR Scan cancelled")
                Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("MainActivity_QR", "QR Scan successful: ${result.contents}")
                handleScannedQrCode(result.contents)
            }
        }

        setContent {
            PurrytifyTheme {
                AppNavigation(
                    // Teruskan instance ViewModel dari MainActivity
                    songViewModel = this.songViewModel,
                    playerViewModel = this.playerViewModel,
                    onlineSongViewModel = this.onlineSongViewModel,
                    onScanQrClicked = { launchQrScanner() }
                )
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity_Lifecycle", "onNewIntent called with intent: $intent")
        intent?.let {
            setIntent(it) // Penting untuk update intent Activity jika dipanggil saat sudah berjalan
            handleIntent(it)
        }
    }

    fun launchQrScanner() {
        Log.d("MainActivity_QR", "launchQrScanner() CALLED by UI")
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a Purrytify Song QR Code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        qrScanLauncher.launch(options)
    }

    private fun handleScannedQrCode(scannedData: String) {
        Log.d("MainActivity_DeepLink", "Scanned QR Data: $scannedData")
        try {
            val scannedUri = Uri.parse(scannedData)
            if (scannedUri.scheme == "purrytify" && scannedUri.host == "song") {
                val intent = Intent(Intent.ACTION_VIEW, scannedUri)
                handleIntent(intent) // Panggil handleIntent yang sudah ada
            } else {
                Log.w("MainActivity_DeepLink", "Scanned QR is not a valid Purrytify song link: $scannedData")
                Toast.makeText(this, "Invalid Purrytify QR Code", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity_DeepLink", "Error parsing scanned QR data: $scannedData", e)
            Toast.makeText(this, "Error reading QR Code", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleIntent(intent: Intent) {
        val action: String? = intent.action
        val data: Uri? = intent.data
        Log.d("MainActivity_DeepLink", "Handling Intent - Action: $action, Data: $data. Using SongVM hash: ${System.identityHashCode(songViewModel)}")

        if (Intent.ACTION_VIEW == action && data != null) {
            val scheme = data.scheme
            val host = data.host
            val pathSegments = data.pathSegments

            Log.d("MainActivity_DeepLink", "Intent URI - Scheme: $scheme, Host: $host, Segments: $pathSegments")

            if (scheme == "purrytify" && host == "song" && pathSegments.isNotEmpty()) {
                val songIdString = pathSegments.first()
                val songId = songIdString.toIntOrNull()

                if (songId != null) {
                    Log.i("MainActivity_DeepLink", "Processing song ID from deep link/QR: $songId")
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val songToPlay = onlineSongViewModel.fetchSongById(songId) // Gunakan instance dari Activity
                            if (songToPlay != null) {
                                Log.i("MainActivity_DeepLink", "Song fetched: ${songToPlay.title}. Calling setCurrentSong on VM hash: ${System.identityHashCode(songViewModel)}")
                                songViewModel.setCurrentSong(songToPlay) // Gunakan instance dari Activity
                                songToPlay.audioPath.let { audioPathString ->
                                    try {
                                        val audioUri = audioPathString.toUri()
                                        playerViewModel.prepareAndPlay(audioUri) {} // Gunakan instance dari Activity
                                        // TODO: Secara otomatis tampilkan UI player
                                    } catch (e: Exception) {
                                        Log.e("MainActivity_DeepLink", "Invalid audio path URI: $audioPathString", e)
                                        Toast.makeText(this@MainActivity, "Error playing song: Invalid audio path", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Log.e("MainActivity_DeepLink", "Failed to fetch song with ID: $songId from server.")
                                Toast.makeText(this@MainActivity, "Song not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity_DeepLink", "Error processing song ID $songId: ${e.message}", e)
                            Toast.makeText(this@MainActivity, "Error processing song", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("MainActivity_DeepLink", "Invalid song ID format in deep link/QR: $songIdString")
                    Toast.makeText(this, "Invalid song ID in QR", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("MainActivity_DeepLink", "Intent data not a Purrytify song link. Scheme: $scheme, Host: $host")
            }
        } else {
             Log.d("MainActivity_DeepLink", "Intent not for VIEW action or no data. Action: $action")
        }
    }
}