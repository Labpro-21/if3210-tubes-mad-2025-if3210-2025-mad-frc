package com.example.purrytify.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.R
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.LoginRepository
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.utils.TokenManager
import com.example.purrytify.viewmodel.LoginViewModel
import com.example.purrytify.ui.LockScreenOrientation
import android.content.pm.ActivityInfo
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.example.purrytify.model.User
import com.example.purrytify.viewmodel.LoginViewModelFactory
import com.example.purrytify.utils.SessionManager


@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(
            LocalContext.current.applicationContext as Application,
            LoginRepository(),
        )
    ),
    onLoginSuccess: (accessToken: String) -> Unit = {},
    isConnected: Boolean
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    var showNoInternetDialog by remember { mutableStateOf(!isConnected) }
    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val uiState = viewModel.uiState.value
    val isLoading = viewModel.isLoading.value
    val loginResult = viewModel.loginResult.value
    val userRepository = UserRepository(userDao = AppDatabase.getDatabase(LocalContext.current).userDao())
    val sessionManager = remember { SessionManager(context) }

    val focusManager = LocalFocusManager.current



    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }


    var passwordVisible by remember { mutableStateOf(false) }


    LaunchedEffect(loginResult) {
        loginResult?.let { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { loginResponse ->
                    tokenManager.saveTokens(
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )
                    if (!userRepository.isEmailRegistered(uiState.email)) {

                        val newUser = User(
                            email = uiState.email,
                            songs = 0,
                            likedSongs = 0,
                            listenedSongs = 0
                        )
                        userRepository.insertUser(newUser)
                    }
                    val userId = userRepository.getUserIdByEmail(uiState.email) ?: -1
                    sessionManager.clearSession()
                    sessionManager.saveSession(userId)
                    onLoginSuccess(loginResponse.accessToken)
                    viewModel.clearLoginResult()

                    emailError = null
                    passwordError = null
                }
            } else {

                val errorText = result.exceptionOrNull()?.message ?: "Unknown error"
                when {
                    errorText.contains("400", ignoreCase = true) ->
                        emailError = "Invalid input"
                    errorText.contains("401", ignoreCase = true) ->
                        passwordError = "Invalid credential"
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues)

                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .offset(y = -120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_login),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(24.dp))


                Text(
                    text = "Millions of Songs. Only on Purritify.",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W700
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))


                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = {
                        viewModel.updateEmail(it)
                        emailError = null
                    },
                    label = { Text("Email", color = Color.White) },
                    placeholder = { Text("Email", color = Color(0xFFB3B3B3)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        backgroundColor = Color(0xFF212121),
                        placeholderColor = Color(0xFFB3B3B3),
                        focusedBorderColor = Color(0xFF535353),
                        unfocusedBorderColor = Color(0xFF535353),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    })
                )

                emailError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        style = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = {
                        viewModel.updatePassword(it)
                        passwordError = null
                    },
                    label = { Text("Password", color = Color.White) },
                    placeholder = { Text("Password", color = Color(0xFFB3B3B3)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        backgroundColor = Color(0xFF212121),
                        placeholderColor = Color(0xFFB3B3B3),
                        focusedBorderColor = Color(0xFF535353),
                        unfocusedBorderColor = Color(0xFF535353),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.White
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.login()
                    })
                )

                passwordError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        style = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1db955))
                ) {
                    Text("Log In")
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }
}