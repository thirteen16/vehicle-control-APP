package com.example.app.ui.command

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.CommandRepository
import kotlinx.coroutines.launch

class CommandHistoryViewModel(
    private val commandRepository: CommandRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(CommandHistoryUiState(isLoading = true))
    val uiState: LiveData<CommandHistoryUiState> = _uiState

    init {
        loadDefaultHistory()
    }

    fun loadDefaultHistory() {
        viewModelScope.launch {
            val selectedVehicleId = commandRepository.getSelectedVehicleId()
            if (selectedVehicleId.isNullOrBlank()) {
                performLoad(
                    vehicleId = null,
                    isAllHistory = true,
                    infoMessage = "当前未选择车辆，已显示全部历史"
                )
            } else {
                performLoad(
                    vehicleId = selectedVehicleId,
                    isAllHistory = false,
                    infoMessage = null
                )
            }
        }
    }

    fun loadCurrentVehicleHistory() {
        viewModelScope.launch {
            val selectedVehicleId = commandRepository.getSelectedVehicleId()
            if (selectedVehicleId.isNullOrBlank()) {
                performLoad(
                    vehicleId = null,
                    isAllHistory = true,
                    infoMessage = "当前未选择车辆，已显示全部历史"
                )
            } else {
                performLoad(
                    vehicleId = selectedVehicleId,
                    isAllHistory = false,
                    infoMessage = null
                )
            }
        }
    }

    fun loadAllHistory() {
        viewModelScope.launch {
            performLoad(
                vehicleId = null,
                isAllHistory = true,
                infoMessage = null
            )
        }
    }

    private suspend fun performLoad(
        vehicleId: String?,
        isAllHistory: Boolean,
        infoMessage: String?
    ) {
        _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
            isLoading = true,
            selectedVehicleId = vehicleId,
            isAllHistory = isAllHistory,
            infoMessage = null,
            errorMessage = null
        )

        when (val result = commandRepository.getCommandHistory(vehicleId, 50)) {
            is ResultState.Success -> {
                _uiState.value = CommandHistoryUiState(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    isAllHistory = isAllHistory,
                    items = result.data,
                    infoMessage = infoMessage
                )
            }

            is ResultState.Error -> {
                _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    isAllHistory = isAllHistory,
                    errorMessage = result.message
                )
            }

            ResultState.Loading -> {
                _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
                    isLoading = true
                )
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
            infoMessage = null
        )
    }

    fun clearErrorMessage() {
        _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
            errorMessage = null
        )
    }

    class Factory(
        private val commandRepository: CommandRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommandHistoryViewModel(commandRepository) as T
        }
    }
}