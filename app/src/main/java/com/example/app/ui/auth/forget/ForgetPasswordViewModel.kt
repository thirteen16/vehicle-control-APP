package com.example.app.ui.auth.forget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgetPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
        private val CODE_REGEX = Regex("^\\d{6}$")
    }

    private val _uiState = MutableLiveData(ForgetPasswordUiState())
    val uiState: LiveData<ForgetPasswordUiState> = _uiState

    private var countdownJob: Job? = null

    fun sendResetCode(phone: String) {
        val trimPhone = phone.trim()

        if (trimPhone.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "手机号不能为空")
            return
        }

        if (!PHONE_REGEX.matches(trimPhone)) {
            _uiState.value = _uiState.value?.copy(errorMessage = "请输入正确的 11 位手机号")
            return
        }

        if ((_uiState.value?.countdownSeconds ?: 0) > 0) {
            return
        }

        _uiState.value = _uiState.value?.copy(isSendingCode = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = authRepository.sendResetCode(trimPhone)) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isSendingCode = false,
                        sendCodeSuccessMessage = result.data
                    )
                    startCountdown()
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        isSendingCode = false,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isSendingCode = true)
                }
            }
        }
    }

    fun resetPassword(
        phone: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val trimPhone = phone.trim()
        val trimCode = code.trim()
        val trimNewPassword = newPassword.trim()
        val trimConfirmPassword = confirmPassword.trim()

        when {
            trimPhone.isBlank() -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "手机号不能为空")
                return
            }

            !PHONE_REGEX.matches(trimPhone) -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "请输入正确的 11 位手机号")
                return
            }

            trimCode.isBlank() -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "验证码不能为空")
                return
            }

            !CODE_REGEX.matches(trimCode) -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "验证码应为 6 位数字")
                return
            }

            trimNewPassword.isBlank() -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "新密码不能为空")
                return
            }

            trimNewPassword.length < 6 -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "新密码长度不能少于 6 位")
                return
            }

            trimConfirmPassword.isBlank() -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "确认密码不能为空")
                return
            }

            trimNewPassword != trimConfirmPassword -> {
                _uiState.value = _uiState.value?.copy(errorMessage = "两次输入的密码不一致")
                return
            }
        }

        _uiState.value = _uiState.value?.copy(isResetting = true, errorMessage = null)

        viewModelScope.launch {
            when (
                val result = authRepository.resetPassword(
                    phone = trimPhone,
                    code = trimCode,
                    newPassword = trimNewPassword,
                    confirmPassword = trimConfirmPassword
                )
            ) {
                is ResultState.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        isResetting = false,
                        resetSuccess = true,
                        successMessage = result.data
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        isResetting = false,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isResetting = true)
                }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(countdownSeconds = 60)
            for (second in 59 downTo 0) {
                delay(1000)
                _uiState.value = _uiState.value?.copy(countdownSeconds = second)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    fun clearSendCodeSuccessMessage() {
        _uiState.value = _uiState.value?.copy(sendCodeSuccessMessage = null)
    }

    fun clearResetSuccess() {
        _uiState.value = _uiState.value?.copy(
            resetSuccess = false,
            successMessage = null
        )
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ForgetPasswordViewModel(authRepository) as T
        }
    }
}