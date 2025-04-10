 package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.screens.HomeScreenContent
import com.example.purrytify.ui.screens.HomeScreenWithBottomNav
import com.example.purrytify.ui.screens.LibraryScreen
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.ProfileScreen

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
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(route = Screen.Library.route) {
            LibraryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}