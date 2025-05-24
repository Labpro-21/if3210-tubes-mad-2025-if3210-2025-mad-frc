package com.example.purrytify.ui.navigation

import android.R.attr.type
import android.util.Log // Tambahkan import Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.ui.screens.LibraryScreenWithBottomNav
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.ProfileScreenWithBottomNav
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import com.example.purrytify.viewmodel.NetworkViewModel
import com.example.purrytify.utils.TokenManager
import androidx.compose.runtime.LaunchedEffect // Untuk side-effect logging
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.purrytify.ui.screens.HomeScreenResponsive
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.ui.screens.TopScreen
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.viewmodel.OnlineSongViewModelFactory

 sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(onScanQrClicked: () -> Unit, songViewModelFromActivity: SongViewModel) {
    val context = LocalContext.current
    val activity = LocalContext.current as ComponentActivity
    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()
    val db = AppDatabase.getDatabase(context)
    val songRepository = remember { SongRepository(db.songDao(), db.userDao()) } // Renamed to avoid conflict

    val networkViewModel: NetworkViewModel = viewModel()
    val isConnected by networkViewModel.isConnected.observeAsState(initial = true)

    // ----- USER ID HANDLING & SongViewModel CREATION -----
    val currentSessionUserId = sessionManager.getUserId()
    // userIdForViewModel akan menjadi ID user yang valid (positif) atau -1 jika tidak ada sesi.
    // SongViewModel hanya akan dibuat jika userIdForViewModel adalah ID yang valid (positif).
    val userIdForViewModel = currentSessionUserId // Defaultnya -1 jika tidak ada sesi valid

    Log.d("AppNavigation", "Read from SessionManager: currentSessionUserId = $currentSessionUserId. Calculated userIdForViewModel = $userIdForViewModel")

    val startDestination = if (tokenManager.isLoggedIn() && currentSessionUserId > 0) {
        Screen.Home.route
    } else {
        if (tokenManager.isLoggedIn() && currentSessionUserId <= 0) {
            Log.w("AppNavigation", "Token exists but session userId is invalid ($currentSessionUserId). Forcing logout and redirect to Login.")
            tokenManager.clearTokens()
            sessionManager.clearSession()
        }
        Screen.Login.route
    }
    Log.d("AppNavigation", "Determined startDestination: $startDestination")

    val songViewModel: SongViewModel = viewModel(
        key = "songViewModel_${userIdForViewModel}", // Key akan berubah jika userIdForViewModel berubah
        factory = SongViewModelFactory(songRepository, userIdForViewModel) // factory tetap terima userIdForViewModel
    )
    Log.d("AppNavigation", "SongViewModel instance created/obtained with key: songViewModel_${userIdForViewModel}. Passed userId to factory: $userIdForViewModel")


    val playerViewModel: PlayerViewModel = viewModel<PlayerViewModel>(
        factory = PlayerViewModelFactory((context as ComponentActivity).application)
    )

    val api = remember { RetrofitClient.create(tokenManager) } // RetrofitClient mungkin perlu context untuk TokenManager
    val onlineSongViewModel: OnlineSongViewModel = viewModel(
        factory = OnlineSongViewModelFactory(api, sessionManager)
    )
    // ----- END USER ID HANDLING -----

    LaunchedEffect(currentSessionUserId, tokenManager.isLoggedIn()) {
        Log.d("AppNavigation_Recompose", "Recomposing. SessionUserId: $currentSessionUserId, IsLoggedIn: ${tokenManager.isLoggedIn()}")
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                isConnected = isConnected,
                onLoginSuccess = {
                    // Setelah login, userId valid sudah disimpan di sessionManager oleh LoginScreen.
                    // Navigasi akan menyebabkan AppNavigation recompose,
                    // userIdForViewModel akan membaca nilai baru, dan SongViewModel baru akan dibuat.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Home.route) {
            // Sebelum menampilkan Home, kita bisa melakukan pengecekan ulang userId
            val currentHomeUserId = sessionManager.getUserId()
            if (currentHomeUserId <= 0) { // Jika userId tidak valid (misal setelah logout paksa)
                Log.e("AppNavigation_Home", "Accessed Home with invalid userId: $currentHomeUserId. Navigating to Login.")
                // Langsung navigasi kembali ke Login dan bersihkan backstack
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                // userId sudah valid, gunakan SongViewModel yang sudah dibuat dengan userId yang benar
                HomeScreenResponsive(
                    songViewModel = songViewModelFromActivity,
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    playerViewModel = playerViewModel,
                    // onlineViewModel dilewatkan langsung
                    onNavigateToTopSong = { chartType ->
                    navController.navigate("top/$chartType")
                }
            )
            }
        }
        composable(route = Screen.Library.route) {
             val currentLibraryUserId = sessionManager.getUserId()
            if (currentLibraryUserId <= 0) {
                Log.e("AppNavigation_Library", "Accessed Library with invalid userId: $currentLibraryUserId. Navigating to Login.")
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Library.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                LibraryScreenWithBottomNav(
                    onBack = { navController.popBackStack() },
                    songViewModel = songViewModelFromActivity,
                    onNavigateToHome = { navController.navigate(Screen.Home.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    playerViewModel = playerViewModel,
                )
            }
        }
        composable(Screen.Profile.route) {
            val sessionUserId = sessionManager.getUserId()
            if (sessionUserId > 0) {
                val vmToUse: SongViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    key = "songViewModel_user_${sessionUserId}",
                    factory = SongViewModelFactory(songRepository, sessionUserId)
                )
                ProfileScreenWithBottomNav(
                    onNavigateToHome = { navController.navigate(Screen.Home.route) },
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    isConnected = isConnected,
                    onLogout = {
                        playerViewModel.stopPlayer()
                        tokenManager.clearTokens()
                        sessionManager.clearSession()
                        Log.i("AppNavigation_Logout", "User logged out. Navigating to Login.")
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    songViewModel = vmToUse,
                    playerViewModel = playerViewModel,
                    onScanQrClicked = onScanQrClicked // Teruskan callback ke ProfileScreen jika tombol ada di sana
                )
            } else {
                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) { popUpTo(Screen.Profile.route) { inclusive = true }; launchSingleTop = true } }
            }
        }
        composable(
            route = "top/{chartType}",
            arguments = listOf(navArgument("chartType") { type = NavType.StringType })
        ) { backStackEntry ->
            val chartType = backStackEntry.arguments?.getString("chartType") ?: "global"
            TopScreen(
                chartType = chartType,
                onlineViewModel = onlineSongViewModel,
                songViewModel = songViewModelFromActivity,
                playerViewModel = playerViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
    }
}