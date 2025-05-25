package com.example.purrytify.ui.navigation

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.screens.LibraryScreenWithBottomNav
import com.example.purrytify.ui.screens.LoginScreen
import com.example.purrytify.ui.screens.ProfileScreenWithBottomNav
import com.example.purrytify.ui.screens.EditProfileScreen
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.NetworkViewModel
import com.example.purrytify.utils.TokenManager
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.UserRepository
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.ui.screens.HomeScreenResponsive
import com.example.purrytify.ui.screens.NoInternetDialog
import com.example.purrytify.ui.screens.TimeListenedScreen
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.ui.screens.TopScreen
import com.example.purrytify.ui.screens.UserTopArtistsScreen
import com.example.purrytify.ui.screens.UserTopSongsScreen
import com.example.purrytify.viewmodel.ProfileViewModel
import com.example.purrytify.viewmodel.ProfileViewModelFactory
import com.example.purrytify.viewmodel.RecommendationViewModel
import java.time.YearMonth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object TimeListenedDetail : Screen("time_listened_detail")
    object UserTopPlayedSongs : Screen("user_top_played_songs/{yearMonth}") {
        fun createRoute(yearMonth: YearMonth) = "user_top_played_songs/${yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))}"
    }
    object UserTopArtists : Screen("user_top_artists/{yearMonth}") {
        fun createRoute(yearMonth: YearMonth) = "user_top_artists/${yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))}"
    }
}

@Composable
fun AppNavigation(

    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onlineSongViewModel: OnlineSongViewModel,
    onScanQrClicked: () -> Unit,
    audioOutputViewModel: AudioOutputViewModel,
    recommendationViewModel: RecommendationViewModel
) {
    val context = LocalContext.current
    val activity = LocalContext.current as ComponentActivity
    val tokenManager = remember { TokenManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()
    val db = AppDatabase.getDatabase(context)
    val songRepository = remember { SongRepository(db.songDao(), db.userDao()) }
    val userRepository = remember { UserRepository(db.userDao()) }


    val networkViewModel: NetworkViewModel = viewModel()
    val isConnected by networkViewModel.isConnected.observeAsState(initial = true)

    val currentSessionUserId = sessionManager.getUserId()
    Log.d(
        "AppNavigation",
        "SessionManager userId: $currentSessionUserId. Received SongViewModel hash: ${
            System.identityHashCode(songViewModel)
        }"
    )

    val profileViewModel: ProfileViewModel = viewModel(
        viewModelStoreOwner = activity,
        key = "profileViewModel_user_${currentSessionUserId}",
        factory = ProfileViewModelFactory(
            context,
            tokenManager,
            userRepository,
            sessionManager
        )
    )

    val startDestination = if (tokenManager.hasAccessToken() && currentSessionUserId > 0) {
        Screen.Home.route
    } else {
        if (tokenManager.hasAccessToken() && currentSessionUserId <= 0) {
            Log.w(
                "AppNavigation",
                "Token exists but session userId invalid ($currentSessionUserId). Forcing logout."
            )
            LaunchedEffect(Unit) {
                tokenManager.clearTokens()
                sessionManager.clearSession()

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

    val api = remember { RetrofitClient.create(tokenManager) }

    LaunchedEffect(currentSessionUserId, tokenManager.isLoggedIn()) {
        Log.d(
            "AppNavigation_Recompose",
            "Recomposing. SessionUserId: $currentSessionUserId, IsLoggedIn: ${tokenManager.isLoggedIn()}"
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                val homeSessionUserId = sessionManager.getUserId()
                if (homeSessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    Log.e(
                        "AppNavigation_Home",
                        "Accessed Home with invalid session userId: $homeSessionUserId while logged in. Redirecting to Login."
                    )
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    Log.d(
                        "AppNavigation_Home",
                        "Displaying Home for user $homeSessionUserId. SongVM hash: ${
                            System.identityHashCode(songViewModel)
                        }"
                    )
                    HomeScreenResponsive(
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        playerViewModel = playerViewModel,
                        songViewModel = songViewModel,
                        isConnected = isConnected,
                        onlineSongViewModel = onlineSongViewModel,
                        onNavigateToTopSong = { chartType ->
                            navController.navigate("top/$chartType")
                        },
                        audioOutputViewModel = audioOutputViewModel,
                        recommendationViewModel = recommendationViewModel
                    )
                }
            }
            composable(Screen.Library.route) {
                val librarySessionUserId = sessionManager.getUserId()
                if (librarySessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    Log.e(
                        "AppNavigation_Library",
                        "Accessed Library with invalid session userId: $librarySessionUserId while logged in. Redirecting to Login."
                    )
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Library.route) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    Log.d(
                        "AppNavigation_Library",
                        "Displaying Library for user $librarySessionUserId. SongVM hash: ${
                            System.identityHashCode(songViewModel)
                        }"
                    )
                    LibraryScreenWithBottomNav(
                        onBack = { navController.popBackStack() },
                        songViewModel = songViewModel,
                        onNavigateToHome = { navController.navigate(Screen.Home.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        playerViewModel = playerViewModel,
                        isOnline = isConnected,
                        audioOutputViewModel = audioOutputViewModel
                    )
                }
            }
            composable(Screen.Profile.route) {
                val profileSessionUserId = sessionManager.getUserId()
                if (profileSessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    Log.e(
                        "AppNavigation_Profile",
                        "Accessed Profile with invalid session userId: $profileSessionUserId while logged in. Redirecting to Login."
                    )
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Profile.route) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    Log.d(
                        "AppNavigation_Profile",
                        "Displaying Profile for user $profileSessionUserId. SongVM hash: ${
                            System.identityHashCode(songViewModel)
                        }"
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
                        songViewModel = songViewModel,
                        playerViewModel = playerViewModel,
                        onScanQrClicked = onScanQrClicked,
                        onNavigateToTimeListenedDetail = {
                            profileViewModel.loadDailyListenDetailsForMonth(YearMonth.now())
                            navController.navigate(Screen.TimeListenedDetail.route)
                        },
                        onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                        onNavigateToUserTopSongs = { yearMonth ->
                            navController.navigate(Screen.UserTopPlayedSongs.createRoute(yearMonth))
                        },
                        onNavigateToUserTopArtists = { yearMonth ->
                            navController.navigate(Screen.UserTopArtists.createRoute(yearMonth))
                        }
                    )
                }
            }

            composable(Screen.TimeListenedDetail.route) {


                TimeListenedScreen(
                    profileViewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "top/{chartType}",
                arguments = listOf(navArgument("chartType") { type = NavType.StringType })
            ) { backStackEntry ->
                val chartType = backStackEntry.arguments?.getString("chartType") ?: "global"
                TopScreen(
                    chartType = chartType,
                    onlineViewModel = onlineSongViewModel,
                    songViewModel = songViewModel,
                    playerViewModel = playerViewModel,
                    isConnected = isConnected,
                    onBack = { navController.popBackStack() },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    audioOutputViewModel = audioOutputViewModel
                )
            }

            composable(Screen.EditProfile.route) {
                val profileVm: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(
                        context = context,
                        tokenManager = tokenManager,
                        userRepository = userRepository,
                        sessionManager = sessionManager
                    )
                )
                EditProfileScreen(
                    profileViewModel = profileVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.UserTopPlayedSongs.route,
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
            ) { backStackEntry ->
                val yearMonthString =
                    backStackEntry.arguments?.getString("yearMonth") ?: YearMonth.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))

                val sessionUserId = sessionManager.getUserId()
                if (sessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else if (!tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    UserTopSongsScreen(
                        profileViewModel = profileViewModel,
                        songViewModel = songViewModel,
                        playerViewModel = playerViewModel,
                        audioOutputViewModel = audioOutputViewModel,
                        yearMonthString = yearMonthString,
                        onBack = { navController.popBackStack() },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(
                                    Screen.Home.route
                                ) { inclusive = true }
                            }
                        },
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(
                                    Screen.Profile.route
                                ) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.UserTopArtists.route,
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
            ) { backStackEntry ->
                val yearMonthString =
                    backStackEntry.arguments?.getString("yearMonth") ?: YearMonth.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))

                val sessionUserId = sessionManager.getUserId()
                if (sessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else if (!tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    UserTopArtistsScreen(
                        profileViewModel = profileViewModel,
                        songViewModel = songViewModel,
                        playerViewModel = playerViewModel,
                        audioOutputViewModel = audioOutputViewModel,
                        yearMonthString = yearMonthString,
                        onBack = { navController.popBackStack() },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(
                                    Screen.Home.route
                                ) { inclusive = true }
                            }
                        },
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(
                                    Screen.Profile.route
                                ) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.UserTopPlayedSongs.route,
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
            ) { backStackEntry ->
                val yearMonthString =
                    backStackEntry.arguments?.getString("yearMonth") ?: YearMonth.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))

                val sessionUserId = sessionManager.getUserId()
                if (sessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else if (!tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    UserTopSongsScreen(
                        profileViewModel = profileViewModel,
                        songViewModel = songViewModel,
                        playerViewModel = playerViewModel,
                        audioOutputViewModel = audioOutputViewModel,
                        yearMonthString = yearMonthString,
                        onBack = { navController.popBackStack() },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(
                                    Screen.Home.route
                                ) { inclusive = true }
                            }
                        },
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(
                                    Screen.Profile.route
                                ) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.UserTopArtists.route,
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
            ) { backStackEntry ->
                val yearMonthString =
                    backStackEntry.arguments?.getString("yearMonth") ?: YearMonth.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))

                val sessionUserId = sessionManager.getUserId()
                if (sessionUserId <= 0 && tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        tokenManager.clearTokens(); sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else if (!tokenManager.isLoggedIn()) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }; launchSingleTop = true
                        }
                    }
                } else {
                    UserTopArtistsScreen(
                        profileViewModel = profileViewModel,
                        songViewModel = songViewModel,
                        playerViewModel = playerViewModel,
                        audioOutputViewModel = audioOutputViewModel,
                        yearMonthString = yearMonthString,
                        onBack = { navController.popBackStack() },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(
                                    Screen.Home.route
                                ) { inclusive = true }
                            }
                        },
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(
                                    Screen.Profile.route
                                ) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}