 package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.SongRepository
import com.example.purrytify.ui.screens.HomeScreenWithBottomNav
import com.example.purrytify.ui.screens.LibraryScreen
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.ProfileScreen
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory

 // Definisi rute menggunakan sealed class
import com.example.purrytify.ui.screens.*
import com.example.purrytify.viewmodel.NetworkViewModel
import com.example.purrytify.utils.TokenManager
import androidx.compose.runtime.livedata.observeAsState

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
    val navController = rememberNavController()
    val db = AppDatabase.getDatabase(context)
    val repository = remember { SongRepository(db.songDao()) }

    val songViewModel: SongViewModel = viewModel(factory = SongViewModelFactory(repository))
    val networkViewModel: NetworkViewModel = viewModel()
    val isConnected by networkViewModel.isConnected.observeAsState(initial = true)
    println("Is Connected: $isConnected")

    // Tentukan startDestination berdasarkan status login dan koneksi internet
    val startDestination = when {
        tokenManager.isLoggedIn() -> Screen.Home.route
        else -> Screen.Login.route
    }

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
                }
            )
        }
        composable(route = Screen.Home.route) {
            HomeScreenWithBottomNav(
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                songViewModel = songViewModel
            )
        }
        composable(route = Screen.Library.route) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                songViewModel = songViewModel
            )
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                isConnected = isConnected,
                onLogout = {
                    // Implementasi onLogout:
                    tokenManager.clearTokens()
                    // Navigasi kembali ke layar login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}