package com.example.purrytify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.LoginResponseNetwork
import com.example.purrytify.network.RetrofitInstance
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val response: LoginResponseNetwork) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    fun loginUser(email: String, password: String) {
        if (!isValidCredentials(email, password)) {
            _loginState.value = LoginState.Error("Format email atau password salah")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.login(
                    email = email,
                    password = password
                )

                when {
                    response.isSuccessful && response.body() != null -> {
                        val loginResponse = response.body()!!
                        tokenManager.saveTokens(
                            accessToken = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken
                        )
                        _loginState.postValue(LoginState.Success(loginResponse))
                    }

                    response.code() == 401 -> {
                        _loginState.postValue(LoginState.Error("Autentikasi gagal: NIM atau password salah"))
                    }

                    else -> {
                        _loginState.postValue(LoginState.Error("Error ${response.code()}: ${response.message()}"))
                    }
                }
            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: IOException) {
                _loginState.postValue(LoginState.Error("Koneksi jaringan bermasalah"))
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error("Terjadi kesalahan tak terduga"))
            }
        }
    }

    private fun isValidCredentials(email: String, password: String): Boolean {
        val nimRegex = """^\d+@std\.stei\.itb\.ac\.id${'$'}""".toRegex()
        return email.matches(nimRegex) && password.matches(Regex("""^\d+${'$'}"""))
    }

    private fun handleHttpException(e: HttpException) {
        val errorMessage = when (e.code()) {
            400 -> "Request tidak valid"
            403 -> "Akses ditolak"
            500 -> "Server mengalami masalah"
            else -> "Error HTTP ${e.code()}"
        }
        _loginState.postValue(LoginState.Error(errorMessage))
    }
}