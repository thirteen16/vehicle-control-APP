package com.example.app.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    init {
        loadRememberedLoginInfo()
    }

    private fun loadRememberedLoginInfo() {
        viewModelScope.launch {
            val info = authRepository.getRememberedLoginInfo()
            _uiState.value = _uiState.value?.copy(
                rememberedAccount = info.account,
                rememberedPassword = info.password,
                rememberPassword = info.rememberPassword,
                autoLoginEnabled = info.autoLoginEnabled,
                rememberedInfoLoaded = true
            )
        }
    }

    fun checkAutoLogin() {
        viewModelScope.launch {
            if (authRepository.canAutoLogin()) {
                _uiState.value = _uiState.value?.copy(autoLogin = true)
            }
        }
    }

    fun login(
        account: String,
        password: String,
        rememberPassword: Boolean,
        autoLoginEnabled: Boolean
    ) {
        val trimAccount = account.trim()
        val trimPassword = password.trim()

        if (trimAccount.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "用户名或手机号不能为空")
            return
        }

        if (trimPassword.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "密码不能为空")
            return
        }

        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            when (
                val result = authRepository.login(
                    account = trimAccount,
                    password = trimPassword,
                    rememberPassword = rememberPassword,
                    autoLoginEnabled = autoLoginEnabled
                )
            ) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        loginSuccess = true,
                        loginData = result.data
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isLoading = true)
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
            return LoginViewModel(authRepository) as T
        }
    }
}