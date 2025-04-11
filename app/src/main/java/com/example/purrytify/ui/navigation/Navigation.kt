 package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
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
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object Player : Screen("player")
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val repository = remember { SongRepository(db.songDao()) }

    val songViewModel: SongViewModel = viewModel(factory = SongViewModelFactory(repository))

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        // Pastikan agar Login dihapus dari backstack, sehingga pengguna tidak kembali ke halaman login
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}