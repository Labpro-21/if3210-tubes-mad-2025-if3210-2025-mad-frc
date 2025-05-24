 package com.example.purrytify.ui.navigation

import android.app.Activity
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
import com.example.purrytify.ui.screens.HomeScreenWithBottomNav
import com.example.purrytify.ui.screens.LibraryScreenWithBottomNav
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.ProfileScreenWithBottomNav
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory

// Definisi rute menggunakan sealed class
import com.example.purrytify.viewmodel.NetworkViewModel
import com.example.purrytify.utils.TokenManager
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.purrytify.repository.UserRepository
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
    object Player : Screen("player")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()
    val db = AppDatabase.getDatabase(context)
    val repository = remember { SongRepository(db.songDao(), db.userDao()) }

    val userId = sessionManager.getUserId() ?: 0
    val songViewModel: SongViewModel = viewModel(
        key = "songViewModel_${userId}",
        factory = SongViewModelFactory(repository, userId, (context as ComponentActivity).application)
    )
    val networkViewModel: NetworkViewModel = viewModel()
    val isConnected by networkViewModel.isConnected.observeAsState(initial = true)
    println("Is Connected: $isConnected")

    // Tentukan startDestination berdasarkan status login dan koneksi internet
    val startDestination = when {
        tokenManager.isLoggedIn() -> Screen.Home.route
        else -> Screen.Login.route
    }

    val playerViewModel: PlayerViewModel = viewModel<PlayerViewModel>(
        factory = PlayerViewModelFactory(context.application)
    )

    // Tambahkan OnlineSongViewModel
    val api = remember { RetrofitClient.create(tokenManager) }
    val onlineViewModel: OnlineSongViewModel = viewModel(
        factory = OnlineSongViewModelFactory(api, sessionManager,context.application)
    )

    NavHost(navController = navController, startDestination = startDestination) {
        println("is Connected: $isConnected")
        composable(route = Screen.Login.route) {
            LoginScreen(
                isConnected = isConnected,
                onLoginSuccess = { accessToken ->
                    // Setelah login, navigasi ke Home dan hapus Login dari backstack
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    songViewModel.loadSongs(sessionManager.getUserId())
                }
            )
        }
        composable(route = Screen.Home.route) {
            HomeScreenResponsive(
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
            )
        }
        composable(route = Screen.Library.route) {
            LibraryScreenWithBottomNav(
                onBack = { navController.popBackStack() },
                songViewModel = songViewModel,
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                playerViewModel = playerViewModel,
            )
        }
        composable(route = Screen.Profile.route) {
            ProfileScreenWithBottomNav(
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                isConnected = isConnected,
                onLogout = {
                    playerViewModel.stopPlayer()
                    tokenManager.clearTokens()
                    sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                    songViewModel.reset()
                },
                songViewModel = songViewModel
            )
        }
    }
}