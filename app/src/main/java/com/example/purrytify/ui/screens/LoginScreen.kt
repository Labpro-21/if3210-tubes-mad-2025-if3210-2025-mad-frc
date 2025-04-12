package com.example.purrytify.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.R
import com.example.purrytify.utils.TokenManager
import com.example.purrytify.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (accessToken: String) -> Unit = {},
    isConnected: Boolean
) {
    var showNoInternetDialog by remember { mutableStateOf(!isConnected) }
    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false })
    }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val uiState = viewModel.uiState.value
    val isLoading = viewModel.isLoading.value
    val loginResult = viewModel.loginResult.value

    // State untuk error message di tiap field
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Untuk toggle tampilan password
    var passwordVisible by remember { mutableStateOf(false) }

    // Jika login sukses, simpan token dan panggil callback
    LaunchedEffect(loginResult) {
        loginResult?.let { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { loginResponse ->
                    tokenManager.saveTokens(
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )
                    onLoginSuccess(loginResponse.accessToken)
                    viewModel.clearLoginResult()
                    // Bersihkan error jika ada
                    emailError = null
                    passwordError = null
                }
            } else {
                // Jika login gagal, periksa pesan error
                val errorText = result.exceptionOrNull()?.message ?: "Unknown error"
                when {
                    errorText.contains("400", ignoreCase = true) ->
                        emailError = "Invalid input"
                    errorText.contains("401", ignoreCase = true) ->
                        passwordError = "Invalid credential"
                    else -> {
                        // Bisa juga menampilkan error umum di dialog
                    }
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
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_login),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .offset(y = -120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Slogan/Jargon
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

                // Input Email
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = {
                        viewModel.updateEmail(it)
                        emailError = null  // reset error saat berubah
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
                    shape = RoundedCornerShape(4.dp)
                )
                // Tampilkan error untuk email jika ada
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

                // Input Password dengan toggle icon
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
                    }
                )
                // Tampilkan error untuk password jika ada
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

                // Tombol Login
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