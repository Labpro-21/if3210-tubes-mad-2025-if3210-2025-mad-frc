package com.example.purrytify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.viewmodel.LoginViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.content.Context

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (accessToken: String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.value
    val isLoading = viewModel.isLoading.value
    val loginResult = viewModel.loginResult.value

    // Mengamati loginResult dan jika sukses, simpan token dan panggil callback
    LaunchedEffect(loginResult) {
        loginResult?.let { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { response ->
                    // Simpan token menggunakan SharedPreferences
                    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                    prefs.edit().putString("accessToken", response.accessToken).apply()

                    // Panggil callback untuk navigasi ke Home
                    onLoginSuccess(response.accessToken)
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

            // Tampilkan dialog jika login gagal (opsional)
            loginResult?.let { result ->
                if(result.isFailure){
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}