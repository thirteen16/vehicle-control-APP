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

    fun checkAutoLogin() {
        viewModelScope.launch {
            val rememberedInfo = authRepository.getRememberedLoginInfo()

            _uiState.value = _uiState.value?.copy(
                rememberedAccount = rememberedInfo.account,
                rememberedPassword = rememberedInfo.password,
                rememberPassword = rememberedInfo.rememberPassword || rememberedInfo.autoLoginEnabled,
                autoLoginEnabled = rememberedInfo.autoLoginEnabled,
                rememberedInfoLoaded = true,
                errorMessage = null,
                infoMessage = null
            )

            if (!rememberedInfo.autoLoginEnabled) {
                return@launch
            }

            _uiState.value = _uiState.value?.copy(
                isLoading = true,
                loginSuccess = false,
                autoLogin = false,
                errorMessage = null,
                infoMessage = null
            )

            if (authRepository.hasSavedToken()) {
                when (val validateResult = authRepository.validateSavedLoginSession()) {
                    is ResultState.Success -> {
                        _uiState.value = _uiState.value?.copy(
                            isLoading = false,
                            autoLogin = true,
                            errorMessage = null,
                            infoMessage = null
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
                                errorMessage = validateResult.message,
                                infoMessage = null
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
                        errorMessage = null,
                        infoMessage = null
                    )
                }

                is ResultState.Error -> {
                    authRepository.disableAutoLogin()

                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        autoLogin = false,
                        rememberPassword = rememberedInfo.rememberPassword,
                        autoLoginEnabled = false,
                        errorMessage = "自动登录失败，请手动登录：" + loginResult.message,
                        infoMessage = null
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
            _uiState.value = _uiState.value?.copy(errorMessage = "账号不能为空")
            return
        }

        if (trimPassword.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "密码不能为空")
            return
        }

        val finalRememberPassword = rememberPassword || autoLoginEnabled

        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            errorMessage = null,
            infoMessage = null,
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
                        errorMessage = null,
                        infoMessage = null
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        loginSuccess = false,
                        errorMessage = result.message,
                        infoMessage = null
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isLoading = true)
                }
            }
        }
    }

    fun sendLoginCode(phone: String) {
        val trimPhone = phone.trim()

        if (!isValidPhone(trimPhone)) {
            _uiState.value = _uiState.value?.copy(errorMessage = "请输入正确的11位手机号")
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.sendLoginCode(trimPhone)) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        errorMessage = null,
                        infoMessage = result.data.ifBlank { "验证码发送成功" }
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        errorMessage = result.message,
                        infoMessage = null
                    )
                }

                ResultState.Loading -> Unit
            }
        }
    }

    fun smsLogin(phone: String, code: String) {
        val trimPhone = phone.trim()
        val trimCode = code.trim()

        if (!isValidPhone(trimPhone)) {
            _uiState.value = _uiState.value?.copy(errorMessage = "请输入正确的11位手机号")
            return
        }

        if (!trimCode.matches(Regex("^\\d{6}$"))) {
            _uiState.value = _uiState.value?.copy(errorMessage = "验证码必须是6位数字")
            return
        }

        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            errorMessage = null,
            infoMessage = null,
            loginSuccess = false,
            autoLogin = false
        )

        viewModelScope.launch {
            when (val result = authRepository.smsLogin(trimPhone, trimCode)) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        loginSuccess = true,
                        loginData = result.data,
                        rememberPassword = false,
                        autoLoginEnabled = false,
                        errorMessage = null,
                        infoMessage = null
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        loginSuccess = false,
                        errorMessage = result.message,
                        infoMessage = null
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isLoading = true)
                }
            }
        }
    }

    fun clearTransientMessages() {
        _uiState.value = _uiState.value?.copy(
            errorMessage = null,
            infoMessage = null
        )
    }

    fun clearError() {
        clearTransientMessages()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
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
