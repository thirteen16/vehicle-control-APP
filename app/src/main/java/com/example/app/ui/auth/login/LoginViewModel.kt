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
                rememberPassword = info.rememberPassword || info.autoLoginEnabled,
                autoLoginEnabled = info.autoLoginEnabled,
                rememberedInfoLoaded = true
            )
        }
    }

    /**
     * 自动登录完整逻辑：
     *
     * 1. 没有勾选自动登录：不自动进入主页；
     * 2. 勾选了自动登录，并且本地 token 有效：直接进入主页；
     * 3. 勾选了自动登录，但 token 过期：使用保存的账号密码重新登录；
     * 4. 重新登录成功：保存新 token，并进入主页；
     * 5. 重新登录失败：关闭自动登录，停留在登录页。
     */
    fun checkAutoLogin() {
        viewModelScope.launch {
            val rememberedInfo = authRepository.getRememberedLoginInfo()

            _uiState.value = _uiState.value?.copy(
                rememberedAccount = rememberedInfo.account,
                rememberedPassword = rememberedInfo.password,
                rememberPassword = rememberedInfo.rememberPassword || rememberedInfo.autoLoginEnabled,
                autoLoginEnabled = rememberedInfo.autoLoginEnabled,
                rememberedInfoLoaded = true,
                errorMessage = null
            )

            if (!rememberedInfo.autoLoginEnabled) {
                return@launch
            }

            _uiState.value = _uiState.value?.copy(
                isLoading = true,
                loginSuccess = false,
                autoLogin = false,
                errorMessage = null
            )

            if (authRepository.hasSavedToken()) {
                when (val validateResult = authRepository.validateSavedLoginSession()) {
                    is ResultState.Success -> {
                        _uiState.value = _uiState.value?.copy(
                            isLoading = false,
                            autoLogin = true,
                            errorMessage = null
                        )
                        return@launch
                    }

                    is ResultState.Error -> {
                        val shouldTryPasswordLogin = validateResult.code == 401 ||
                                validateResult.code == 403 ||
                                validateResult.code != null

                        if (!shouldTryPasswordLogin) {
                            _uiState.value = _uiState.value?.copy(
                                isLoading = false,
                                autoLogin = false,
                                errorMessage = validateResult.message
                            )
                            return@launch
                        }
                    }

                    ResultState.Loading -> {
                        _uiState.value = _uiState.value?.copy(isLoading = true)
                    }
                }
            }

            when (val loginResult = authRepository.autoLoginWithSavedCredentials()) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        autoLogin = true,
                        loginData = loginResult.data,
                        rememberPassword = true,
                        autoLoginEnabled = true,
                        errorMessage = null
                    )
                }

                is ResultState.Error -> {
                    authRepository.disableAutoLogin()

                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        autoLogin = false,
                        rememberPassword = rememberedInfo.rememberPassword,
                        autoLoginEnabled = false,
                        errorMessage = "自动登录失败，请手动登录：" + loginResult.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isLoading = true)
                }
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
            _uiState.value = _uiState.value?.copy(
                errorMessage = "用户名或手机号不能为空"
            )
            return
        }

        if (trimPassword.isBlank()) {
            _uiState.value = _uiState.value?.copy(
                errorMessage = "密码不能为空"
            )
            return
        }

        val finalRememberPassword = rememberPassword || autoLoginEnabled

        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            errorMessage = null,
            loginSuccess = false,
            autoLogin = false
        )

        viewModelScope.launch {
            when (
                val result = authRepository.login(
                    account = trimAccount,
                    password = trimPassword,
                    rememberPassword = finalRememberPassword,
                    autoLoginEnabled = autoLoginEnabled
                )
            ) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        loginSuccess = true,
                        loginData = result.data,
                        rememberPassword = finalRememberPassword,
                        autoLoginEnabled = autoLoginEnabled,
                        errorMessage = null
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        loginSuccess = false,
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