package com.example.app.ui.command

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.model.response.CommandHistoryItemResponse
import com.example.app.data.repository.CommandRepository
import kotlinx.coroutines.launch

class CommandHistoryViewModel(
    private val commandRepository: CommandRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(CommandHistoryUiState(isLoading = true))
    val uiState: LiveData<CommandHistoryUiState> = _uiState

    private var sourceItems: List<CommandHistoryItemResponse> = emptyList()
    private var currentVehicleId: String? = null
    private var currentIsAllHistory: Boolean = false

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

    fun setResultFilter(filter: CommandResultFilter) {
        val current = _uiState.value ?: CommandHistoryUiState()
        val filtered = applyResultFilter(sourceItems, filter)

        _uiState.value = current.copy(
            resultFilter = filter,
            items = filtered,
            totalLoadedCount = sourceItems.size,
            infoMessage = null,
            errorMessage = null
        )
    }

    private suspend fun performLoad(
        vehicleId: String?,
        isAllHistory: Boolean,
        infoMessage: String?
    ) {
        val currentFilter = _uiState.value?.resultFilter ?: CommandResultFilter.ALL

        _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
            isLoading = true,
            selectedVehicleId = vehicleId,
            isAllHistory = isAllHistory,
            infoMessage = null,
            errorMessage = null
        )

        when (val result = commandRepository.getCommandHistory(vehicleId, 50)) {
            is ResultState.Success -> {
                currentVehicleId = vehicleId
                currentIsAllHistory = isAllHistory
                sourceItems = result.data

                _uiState.value = CommandHistoryUiState(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    isAllHistory = isAllHistory,
                    resultFilter = currentFilter,
                    items = applyResultFilter(sourceItems, currentFilter),
                    totalLoadedCount = sourceItems.size,
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

    private fun applyResultFilter(
        list: List<CommandHistoryItemResponse>,
        filter: CommandResultFilter
    ): List<CommandHistoryItemResponse> {
        return when (filter) {
            CommandResultFilter.ALL -> list
            CommandResultFilter.SUCCESS -> list.filter {
                it.result.equals("SUCCESS", ignoreCase = true)
            }

            CommandResultFilter.FAILED -> list.filter {
                it.result.equals("FAILED", ignoreCase = true)
            }

            CommandResultFilter.PENDING -> list.filter {
                it.result.equals("PENDING", ignoreCase = true)
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