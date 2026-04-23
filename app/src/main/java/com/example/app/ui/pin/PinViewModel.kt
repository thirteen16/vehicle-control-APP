package com.example.app.ui.pin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app.data.local.PinStore

class PinViewModel(
    private val pinStore: PinStore
) : ViewModel() {

    private val _uiState = MutableLiveData(PinUiState(hasPin = pinStore.hasPin()))
    val uiState: LiveData<PinUiState> = _uiState

    fun refreshStatus() {
        _uiState.value = (_uiState.value ?: PinUiState()).copy(
            hasPin = pinStore.hasPin()
        )
    }

    fun savePin(pin: String, confirmPin: String) {
        val purePin = pin.trim()
        val pureConfirmPin = confirmPin.trim()

        when {
            purePin.isBlank() || pureConfirmPin.isBlank() -> {
                _uiState.value = current().copy(errorMessage = "请输入完整 PIN")
                return
            }

            purePin.length < 4 -> {
                _uiState.value = current().copy(errorMessage = "PIN 至少 4 位")
                return
            }

            purePin.length > 8 -> {
                _uiState.value = current().copy(errorMessage = "PIN 最多 8 位")
                return
            }

            !purePin.all { it.isDigit() } -> {
                _uiState.value = current().copy(errorMessage = "PIN 只能是数字")
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

        if (purePin.isBlank()) {
            _uiState.value = current().copy(errorMessage = "请输入 PIN")
            return
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
                errorMessage = "PIN 错误"
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