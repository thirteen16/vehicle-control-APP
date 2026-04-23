package com.example.app.ui.control

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.model.response.WsCommandAckData
import com.example.app.data.model.response.WsVehicleStateData
import com.example.app.data.repository.CommandRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlViewModel(
    private val commandRepository: CommandRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(ControlUiState(isLoading = true))
    val uiState: LiveData<ControlUiState> = _uiState

    private var pollingJob: Job? = null

    init {
        loadSelectedVehicle()
    }

    fun loadSelectedVehicle() {
        viewModelScope.launch {
            val vehicleId = commandRepository.getSelectedVehicleId()
            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                isLoading = false,
                selectedVehicleId = vehicleId
            )
        }
    }

    fun applyRealtimeConnection(connected: Boolean) {
        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
            wsConnected = connected
        )
    }

    fun applyRealtimeCommandAck(data: WsCommandAckData) {
        val current = _uiState.value ?: ControlUiState()
        val selectedVehicleId = current.selectedVehicleId

        if (!selectedVehicleId.isNullOrBlank() &&
            !data.vehicleId.isNullOrBlank() &&
            data.vehicleId != selectedVehicleId
        ) {
            return
        }

        pollingJob?.cancel()

        _uiState.value = current.copy(
            isPolling = false,
            lastCommandId = data.commandId ?: current.lastCommandId,
            lastCommandType = data.type ?: current.lastCommandType,
            lastCommandResult = data.result ?: current.lastCommandResult,
            lastRequestTime = data.requestTime ?: current.lastRequestTime,
            lastResponseTime = data.responseTime ?: current.lastResponseTime
        )
    }

    fun applyRealtimeVehicleState(data: WsVehicleStateData) {
        val current = _uiState.value ?: ControlUiState()
        val selectedVehicleId = current.selectedVehicleId

        if (!selectedVehicleId.isNullOrBlank() &&
            !data.vehicleId.isNullOrBlank() &&
            data.vehicleId != selectedVehicleId
        ) {
            return
        }

        _uiState.value = current.copy(
            latestVehicleStateText = buildVehicleStateSummary(data)
        )
    }

    fun sendCommand(type: String) {
        val vehicleId = _uiState.value?.selectedVehicleId
        if (vehicleId.isNullOrBlank()) {
            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                errorMessage = "请先在车辆页选择一辆车"
            )
            return
        }

        pollingJob?.cancel()

        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
            isLoading = true,
            isPolling = false,
            errorMessage = null,
            infoMessage = null
        )

        viewModelScope.launch {
            when (val result = commandRepository.submitCommand(vehicleId, type)) {
                is ResultState.Success -> {
                    val data = result.data
                    val commandId = data.commandId

                    _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                        isLoading = false,
                        lastCommandId = commandId,
                        lastCommandType = data.type ?: type,
                        lastCommandResult = data.result ?: "PENDING",
                        lastRequestTime = data.requestTime ?: data.createdTime,
                        lastResponseTime = data.responseTime,
                        infoMessage = "命令已发送"
                    )

                    if (!commandId.isNullOrBlank()) {
                        pollCommandResult(commandId)
                    }
                }

                is ResultState.Error -> {
                    _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                        isLoading = false,
                        isPolling = false,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                        isLoading = true
                    )
                }
            }
        }
    }

    fun refreshLastCommandResult() {
        val commandId = _uiState.value?.lastCommandId
        if (commandId.isNullOrBlank()) {
            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                errorMessage = "当前没有可刷新的命令"
            )
            return
        }

        pollingJob?.cancel()
        pollCommandResult(commandId)
    }

    private fun pollCommandResult(commandId: String) {
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                isPolling = true,
                errorMessage = null,
                infoMessage = null
            )

            repeat(20) {
                when (val result = commandRepository.getCommandResult(commandId)) {
                    is ResultState.Success -> {
                        val data = result.data
                        val latestResult = data.result ?: "PENDING"

                        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                            isPolling = latestResult.equals("PENDING", ignoreCase = true),
                            lastCommandId = data.commandId ?: commandId,
                            lastCommandType = data.type ?: _uiState.value?.lastCommandType,
                            lastCommandResult = latestResult,
                            lastRequestTime = data.requestTime,
                            lastResponseTime = data.responseTime
                        )

                        if (!latestResult.equals("PENDING", ignoreCase = true)) {
                            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                                isPolling = false,
                                infoMessage = "命令结果已更新"
                            )
                            return@launch
                        }
                    }

                    is ResultState.Error -> {
                        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                            isPolling = false,
                            errorMessage = result.message
                        )
                        return@launch
                    }

                    ResultState.Loading -> Unit
                }

                delay(500)
            }

            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                isPolling = false,
                infoMessage = "命令仍在等待设备回执，请确认 mock-device 或车机是否在线"
            )
        }
    }

    private fun buildVehicleStateSummary(data: WsVehicleStateData): String {
        return "锁:${data.lockStatus ?: "-"} " +
                "发动机:${data.engineStatus ?: "-"} " +
                "空调:${data.hvacStatus ?: "-"} " +
                "车窗:${data.windowStatus ?: "-"} " +
                "油量:${data.fuelLevel ?: "-"} " +
                "更新时间:${data.updatedTime ?: "-"}"
    }

    fun clearInfoMessage() {
        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
            infoMessage = null
        )
    }

    fun clearErrorMessage() {
        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    class Factory(
        private val commandRepository: CommandRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ControlViewModel(commandRepository) as T
        }
    }
}