package com.example.purrytify.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.viewmodel.LoginViewModel
import com.example.purrytify.utils.TokenManager

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
    // Buat instance TokenManager untuk menyimpan token secara konsisten
    val tokenManager = remember { TokenManager(context) }
    val uiState = viewModel.uiState.value
    val isLoading = viewModel.isLoading.value
    val loginResult = viewModel.loginResult.value

    // Jika login berhasil, simpan token di TokenManager dan panggil callback navigasi
    LaunchedEffect(loginResult) {
        loginResult?.let { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { loginResponse ->
                    // Simpan token menggunakan TokenManager
                    tokenManager.saveTokens(
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )
                    println("Saved Access Token: ${tokenManager.getAccessToken()}")
                    // Panggil callback untuk navigasi
                    onLoginSuccess(loginResponse.accessToken)
                    viewModel.clearLoginResult()
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Login", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Login")
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            // Tampilkan dialog error jika login gagal
            loginResult?.let { result ->
                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    AlertDialog(
                        onDismissRequest = { viewModel.clearLoginResult() },
                        title = { Text("Login Failed") },
                        text = { Text(error) },
                        confirmButton = {
                            Button(onClick = { viewModel.clearLoginResult() }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}