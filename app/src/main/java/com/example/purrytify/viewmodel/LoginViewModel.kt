package com.example.purrytify.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.LoginUiState
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.repository.LoginRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: LoginRepository = LoginRepository()
) : ViewModel() {

    // State untuk email, password, dan data UI login
    private val _uiState = mutableStateOf(LoginUiState())
    val uiState: State<LoginUiState> get() = _uiState

    // State untuk loading API
    var isLoading = mutableStateOf(false)
        private set

    // State hasil login berisi Result dari LoginResponse
    var loginResult = mutableStateOf<Result<LoginResponse>?>(null)
        private set

    // Update email ketika pengguna mengetik
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    // Update password ketika pengguna mengetik
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    // Memanggil repository untuk melakukan login
    fun login() {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.login(_uiState.value.email, _uiState.value.password)
            loginResult.value = result
            isLoading.value = false
        }
    }

    // Mengosongkan hasil login agar dialog tidak terus tampil
    fun clearLoginResult() {
        loginResult.value = null
    }
}