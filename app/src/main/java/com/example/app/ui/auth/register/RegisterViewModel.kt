package com.example.app.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
    }

    private val _uiState = MutableLiveData(RegisterUiState())
    val uiState: LiveData<RegisterUiState> = _uiState

    fun register(
        username: String,
        password: String,
        confirmPassword: String,
        phone: String,
        nickname: String
    ) {
        val trimUsername = username.trim()
        val trimPassword = password.trim()
        val trimConfirmPassword = confirmPassword.trim()
        val trimPhone = phone.trim()
        val trimNickname = nickname.trim()

        when {
            trimUsername.isBlank() -> {
                _uiState.value = RegisterUiState(errorMessage = "用户名不能为空")
                return
            }

            trimPassword.isBlank() -> {
                _uiState.value = RegisterUiState(errorMessage = "密码不能为空")
                return
            }

            trimPassword.length < 6 -> {
                _uiState.value = RegisterUiState(errorMessage = "密码长度不能少于 6 位")
                return
            }

            trimConfirmPassword.isBlank() -> {
                _uiState.value = RegisterUiState(errorMessage = "请再次输入密码")
                return
            }

            trimPassword != trimConfirmPassword -> {
                _uiState.value = RegisterUiState(errorMessage = "两次输入的密码不一致")
                return
            }

            trimPhone.isBlank() -> {
                _uiState.value = RegisterUiState(errorMessage = "手机号不能为空")
                return
            }

            !PHONE_REGEX.matches(trimPhone) -> {
                _uiState.value = RegisterUiState(errorMessage = "请输入正确的 11 位手机号")
                return
            }

            trimNickname.isBlank() -> {
                _uiState.value = RegisterUiState(errorMessage = "昵称不能为空")
                return
            }
        }

        _uiState.value = RegisterUiState(isLoading = true)

        viewModelScope.launch {
            when (
                val result = authRepository.register(
                    username = trimUsername,
                    password = trimPassword,
                    phone = trimPhone,
                    nickname = trimNickname
                )
            ) {
                is ResultState.Success -> {
                    _uiState.value = RegisterUiState(
                        isLoading = false,
                        registerSuccess = true,
                        successMessage = result.data,
                        registeredAccount = trimUsername
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = RegisterUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = RegisterUiState(isLoading = true)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(authRepository) as T
        }
    }
}