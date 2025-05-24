package com.example.purrytify.ui.navigation

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel // Tetap gunakan ini untuk ViewModel yang di-scope ke NavGraph jika perlu
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.screens.LibraryScreenWithBottomNav
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.ProfileScreenWithBottomNav
import com.example.purrytify.viewmodel.SongViewModel // Import classnya
import com.example.purrytify.viewmodel.NetworkViewModel
import com.example.purrytify.utils.TokenManager
import androidx.compose.runtime.livedata.observeAsState
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.ui.screens.HomeScreenResponsive
import com.example.purrytify.ui.screens.TimeListenedScreen
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.PlayerViewModel // Import classnya
import com.example.purrytify.viewmodel.OnlineSongViewModel // Import classnya
import com.example.purrytify.viewmodel.ProfileViewModel
import com.example.purrytify.viewmodel.ProfileViewModelFactory
import com.example.purrytify.viewmodel.AudioDeviceViewModel
import com.example.purrytify.viewmodel.AudioDeviceViewModelFactory
import java.time.YearMonth


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object TimeListenedDetail : Screen("time_listened_detail")
}

@Composable
fun AppNavigation(
    // Terima ViewModel yang sudah dibuat dari MainActivity
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onlineSongViewModel: OnlineSongViewModel,
    onScanQrClicked: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as ComponentActivity
    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()

    val db = remember { AppDatabase.getDatabase(context) }
    val userRepository = remember { UserRepository(db.userDao()) }

    // NetworkViewModel masih bisa dibuat di sini jika hanya digunakan oleh AppNavigation/UI
    val networkViewModel: NetworkViewModel = viewModel()
    val isConnected by networkViewModel.isConnected.observeAsState(initial = true)

    val currentSessionUserId = sessionManager.getUserId()
    Log.d("AppNavigation", "SessionManager userId: $currentSessionUserId. Received SongViewModel hash: ${System.identityHashCode(songViewModel)}")

    val profileViewModel: ProfileViewModel = viewModel(
        viewModelStoreOwner = activity, // activity adalah LocalContext.current as ComponentActivity
        key = "profileViewModel_user_${currentSessionUserId}", // Key jika bergantung pada user
        factory = ProfileViewModelFactory(context, tokenManager, userRepository, sessionManager) // Sesuaikan factory Anda
    )

    val audioDeviceViewModel: AudioDeviceViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = AudioDeviceViewModelFactory(activity.application)
    )

    val startDestination = if (tokenManager.isLoggedIn() && currentSessionUserId > 0) {
        Screen.Home.route
    } else {
        if (tokenManager.isLoggedIn() && currentSessionUserId <= 0) {
            Log.w("AppNavigation", "Token exists but session userId invalid ($currentSessionUserId). Forcing logout.")
            LaunchedEffect(Unit) { // Side effect harus di LaunchedEffect
                tokenManager.clearTokens()
                sessionManager.clearSession()
                // Navigasi ke Login jika startDestination menjadi Login
                if (navController.currentDestination?.route != Screen.Login.route) {
                     navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        Screen.Login.route
    }
    Log.d("AppNavigation", "Determined startDestination: $startDestination")

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                isConnected = isConnected,
                onLoginSuccess = {
                    Log.i("AppNavigation_Login", "Login successful. Navigating to Home.")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            // Validasi userId sebelum menampilkan screen
            val homeSessionUserId = sessionManager.getUserId()
            if (homeSessionUserId <= 0 && tokenManager.isLoggedIn()) { // Jika login tapi sesi aneh
                Log.e("AppNavigation_Home", "Accessed Home with invalid session userId: $homeSessionUserId while logged in. Redirecting to Login.")
                LaunchedEffect(Unit) {
                    tokenManager.clearTokens(); sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Home.route) { inclusive = true }; launchSingleTop = true }
                }
            } else if (!tokenManager.isLoggedIn()){ // Jika tidak login sama sekali
                 LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Home.route) { inclusive = true }; launchSingleTop = true }
                }
            }
            else {
                Log.d("AppNavigation_Home", "Displaying Home for user $homeSessionUserId. SongVM hash: ${System.identityHashCode(songViewModel)}")
                HomeScreenResponsive(
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    songViewModel = songViewModel, // Gunakan instance yang diteruskan
                    playerViewModel = playerViewModel, // Gunakan instance yang diteruskan
                    onlineViewModel = onlineSongViewModel, // Gunakan instance yang diteruskan
                    audioDeviceViewModel = audioDeviceViewModel
                )
            }
        }
        composable(Screen.Library.route) {
            val librarySessionUserId = sessionManager.getUserId()
            if (librarySessionUserId <= 0 && tokenManager.isLoggedIn()) {
                 Log.e("AppNavigation_Library", "Accessed Library with invalid session userId: $librarySessionUserId while logged in. Redirecting to Login.")
                LaunchedEffect(Unit) {
                    tokenManager.clearTokens(); sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Library.route) { inclusive = true }; launchSingleTop = true }
                }
            } else if (!tokenManager.isLoggedIn()){
                 LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Library.route) { inclusive = true }; launchSingleTop = true }
                }
            }
             else {
                Log.d("AppNavigation_Library", "Displaying Library for user $librarySessionUserId. SongVM hash: ${System.identityHashCode(songViewModel)}")
                LibraryScreenWithBottomNav(
                    onBack = { navController.popBackStack() },
                    songViewModel = songViewModel, // Gunakan instance yang diteruskan
                    onNavigateToHome = { navController.navigate(Screen.Home.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    playerViewModel = playerViewModel,
                    audioDeviceViewModel = audioDeviceViewModel
                )
            }
        }
        composable(Screen.Profile.route) {
            val profileSessionUserId = sessionManager.getUserId()
            if (profileSessionUserId <= 0 && tokenManager.isLoggedIn()) {
                Log.e("AppNavigation_Profile", "Accessed Profile with invalid session userId: $profileSessionUserId while logged in. Redirecting to Login.")
                LaunchedEffect(Unit) {
                    tokenManager.clearTokens(); sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Profile.route) { inclusive = true }; launchSingleTop = true }
                }
            } else if (!tokenManager.isLoggedIn()){
                 LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Profile.route) { inclusive = true }; launchSingleTop = true }
                }
            }
            else {
                Log.d("AppNavigation_Profile", "Displaying Profile for user $profileSessionUserId. SongVM hash: ${System.identityHashCode(songViewModel)}")
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
                    songViewModel = songViewModel, // Gunakan instance yang diteruskan
                    playerViewModel = playerViewModel, // Gunakan instance yang diteruskan
                    onScanQrClicked = onScanQrClicked,
                    onNavigateToTimeListenedDetail = {
                        profileViewModel.loadDailyListenDetailsForMonth(YearMonth.now()) // Default ke bulan ini
                        navController.navigate(Screen.TimeListenedDetail.route)
                    },
                )
            }
        }

        composable(Screen.TimeListenedDetail.route) {
            // Asumsi ProfileViewModel sudah di-scope ke Activity atau NavGraph yang lebih tinggi
            // sehingga bisa diakses di sini juga.
            TimeListenedScreen(
                profileViewModel = profileViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}