package com.example.app.ui.pin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app.data.local.PinStore

class PinViewModel(
    private val pinStore: PinStore
) : ViewModel() {

    companion object {
        const val PIN_LENGTH = 6
    }

    private val _uiState = MutableLiveData(PinUiState(hasPin = pinStore.hasPin()))
    val uiState: LiveData<PinUiState> = _uiState

    fun refreshStatus() {
        _uiState.value = current().copy(
            hasPin = pinStore.hasPin()
        )
    }

    fun hasPin(): Boolean {
        return pinStore.hasPin()
    }

    fun isPinCorrect(pin: String): Boolean {
        val purePin = pin.trim()
        return isValidPinFormat(purePin) && pinStore.verifyPin(purePin)
    }

    fun savePin(pin: String, confirmPin: String) {
        val purePin = pin.trim()
        val pureConfirmPin = confirmPin.trim()

        when {
            purePin.isBlank() || pureConfirmPin.isBlank() -> {
                _uiState.value = current().copy(errorMessage = "请输入完整的 6 位 PIN")
                return
            }

            !isValidPinFormat(purePin) -> {
                _uiState.value = current().copy(errorMessage = "PIN 必须是 6 位数字")
                return
            }

            !isValidPinFormat(pureConfirmPin) -> {
                _uiState.value = current().copy(errorMessage = "确认 PIN 必须是 6 位数字")
                return
            }

            purePin != pureConfirmPin -> {
                _uiState.value = current().copy(errorMessage = "两次输入的 PIN 不一致")
                return
            }
        }

        pinStore.savePin(purePin)
        _uiState.value = current().copy(
            hasPin = true,
            saveSuccess = true,
            clearSuccess = false,
            verifySuccess = false,
            infoMessage = "PIN 设置成功",
            errorMessage = null
        )
    }

    fun verifyPin(pin: String) {
        val purePin = pin.trim()

        when {
            !pinStore.hasPin() -> {
                _uiState.value = current().copy(errorMessage = "当前尚未设置 PIN")
                return
            }

            purePin.isBlank() -> {
                _uiState.value = current().copy(errorMessage = "请输入 6 位 PIN")
                return
            }

            !isValidPinFormat(purePin) -> {
                _uiState.value = current().copy(errorMessage = "PIN 必须是 6 位数字")
                return
            }
        }

        if (pinStore.verifyPin(purePin)) {
            _uiState.value = current().copy(
                hasPin = true,
                verifySuccess = true,
                infoMessage = "PIN 验证通过",
                errorMessage = null
            )
        } else {
            _uiState.value = current().copy(
                verifySuccess = false,
                errorMessage = "PIN 错误，请重新输入"
            )
        }
    }

    fun clearPin() {
        if (!pinStore.hasPin()) {
            _uiState.value = current().copy(errorMessage = "当前未设置 PIN")
            return
        }

        pinStore.clearPin()
        _uiState.value = current().copy(
            hasPin = false,
            clearSuccess = true,
            saveSuccess = false,
            verifySuccess = false,
            infoMessage = "PIN 已清除",
            errorMessage = null
        )
    }

    fun clearInfoMessage() {
        _uiState.value = current().copy(infoMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = current().copy(errorMessage = null)
    }

    fun consumeSaveSuccess() {
        _uiState.value = current().copy(saveSuccess = false)
    }

    fun consumeVerifySuccess() {
        _uiState.value = current().copy(verifySuccess = false)
    }

    fun consumeClearSuccess() {
        _uiState.value = current().copy(clearSuccess = false)
    }

    private fun isValidPinFormat(pin: String): Boolean {
        return pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }
    }

    private fun current(): PinUiState {
        return _uiState.value ?: PinUiState(hasPin = pinStore.hasPin())
    }

    class Factory(
        private val pinStore: PinStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PinViewModel(pinStore) as T
        }
    }
}
