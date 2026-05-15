package com.example.app.ui.control

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.model.response.VehicleStateResponse
import com.example.app.data.model.response.WsCommandAckData
import com.example.app.data.model.response.WsVehicleStateData
import com.example.app.data.repository.CommandRepository
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlViewModel(
    private val commandRepository: CommandRepository,
    private val vehicleRepository: VehicleRepository
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
            val current = _uiState.value ?: ControlUiState()

            _uiState.value = current.copy(
                isLoading = true,
                selectedVehicleId = vehicleId,
                errorMessage = null
            )

            if (vehicleId.isNullOrBlank()) {
                _uiState.value = current.copy(
                    isLoading = false,
                    selectedVehicleId = null,
                    onlineStatus = null,
                    lockStatus = null,
                    engineStatus = null,
                    hvacStatus = null,
                    windowStatus = null,
                    latestVehicleStateText = null,
                    errorMessage = "请先在主页选择一辆车"
                )
                return@launch
            }

            refreshSelectedVehicleStateInternal(vehicleId)
        }
    }

    fun refreshSelectedVehicleState() {
        val vehicleId = _uiState.value?.selectedVehicleId
        if (vehicleId.isNullOrBlank()) {
            loadSelectedVehicle()
            return
        }

        viewModelScope.launch {
            val current = _uiState.value ?: ControlUiState()
            _uiState.value = current.copy(
                isLoading = true,
                errorMessage = null
            )
            refreshSelectedVehicleStateInternal(vehicleId)
        }
    }

    private suspend fun refreshSelectedVehicleStateInternal(vehicleId: String) {
        when (val result = vehicleRepository.getVehicleState(vehicleId)) {
            is ResultState.Success -> {
                val current = _uiState.value ?: ControlUiState()
                val state = result.data

                _uiState.value = current.copy(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    onlineStatus = state.onlineStatus,
                    lockStatus = state.lockStatus,
                    engineStatus = state.engineStatus,
                    hvacStatus = state.hvacStatus,
                    windowStatus = state.windowStatus,
                    latestVehicleStateText = buildVehicleStateSummary(state),
                    errorMessage = null
                )
            }

            is ResultState.Error -> {
                val current = _uiState.value ?: ControlUiState()
                _uiState.value = current.copy(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    errorMessage = result.message
                )
            }

            ResultState.Loading -> {
                val current = _uiState.value ?: ControlUiState()
                _uiState.value = current.copy(isLoading = true)
            }
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

        val result = data.result ?: current.lastCommandResult
        val stillPending = result.equals("PENDING", ignoreCase = true)

        if (!stillPending) {
            pollingJob?.cancel()
        }

        _uiState.value = current.copy(
            isPolling = stillPending,
            pendingCommandType = if (stillPending) {
                data.type ?: current.pendingCommandType
            } else {
                null
            },
            lastCommandId = data.commandId ?: current.lastCommandId,
            lastCommandType = data.type ?: current.lastCommandType,
            lastCommandResult = result,
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
            onlineStatus = data.onlineStatus ?: current.onlineStatus,
            lockStatus = data.lockStatus ?: current.lockStatus,
            engineStatus = data.engineStatus ?: current.engineStatus,
            hvacStatus = data.hvacStatus ?: current.hvacStatus,
            windowStatus = data.windowStatus ?: current.windowStatus,
            latestVehicleStateText = buildVehicleStateSummary(data)
        )
    }

    fun sendCommand(type: String) {
        val currentState = _uiState.value ?: ControlUiState()
        val vehicleId = currentState.selectedVehicleId

        if (vehicleId.isNullOrBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "请先在主页选择一辆车"
            )
            return
        }

        /*
         * 普通控制命令仍然要求车辆在线。
         * STATUS_QUERY 是状态查询命令，允许离线时发送，
         * 这样点击刷新也能生成一条“状态查询”历史记录。
         */
        val isStatusQuery = type == "STATUS_QUERY"
        if (!isStatusQuery && currentState.onlineStatus != 1) {
            _uiState.value = currentState.copy(
                isLoading = false,
                isPolling = false,
                pendingCommandType = null,
                errorMessage = "车辆当前离线，无法执行远程控制"
            )
            return
        }

        pollingJob?.cancel()

        _uiState.value = currentState.copy(
            isLoading = true,
            isPolling = false,
            pendingCommandType = type,
            errorMessage = null,
            infoMessage = null
        )

        viewModelScope.launch {
            when (val result = commandRepository.submitCommand(vehicleId, type)) {
                is ResultState.Success -> {
                    val data = result.data
                    val commandId = data.commandId
                    val latestResult = data.result ?: "PENDING"
                    val stillPending = latestResult.equals("PENDING", ignoreCase = true)

                    _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                        isLoading = false,
                        pendingCommandType = if (stillPending) type else null,
                        lastCommandId = commandId,
                        lastCommandType = data.type ?: type,
                        lastCommandResult = latestResult,
                        lastRequestTime = data.requestTime ?: data.createdTime,
                        lastResponseTime = data.responseTime,
                        infoMessage = if (isStatusQuery) "状态查询已记录" else "命令已发送"
                    )

                    if (!commandId.isNullOrBlank()) {
                        pollCommandResult(commandId)
                    }

                    if (isStatusQuery) {
                        refreshSelectedVehicleState()
                    }
                }

                is ResultState.Error -> {
                    _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                        isLoading = false,
                        isPolling = false,
                        pendingCommandType = null,
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
                        val stillPending = latestResult.equals("PENDING", ignoreCase = true)

                        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                            isPolling = stillPending,
                            pendingCommandType = if (stillPending) {
                                data.type ?: _uiState.value?.pendingCommandType
                            } else {
                                null
                            },
                            lastCommandId = data.commandId ?: commandId,
                            lastCommandType = data.type ?: _uiState.value?.lastCommandType,
                            lastCommandResult = latestResult,
                            lastRequestTime = data.requestTime,
                            lastResponseTime = data.responseTime
                        )

                        if (!stillPending) {
                            _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                                isPolling = false,
                                pendingCommandType = null,
                                infoMessage = "命令结果已更新"
                            )
                            return@launch
                        }
                    }

                    is ResultState.Error -> {
                        _uiState.value = (_uiState.value ?: ControlUiState()).copy(
                            isPolling = false,
                            pendingCommandType = null,
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
                pendingCommandType = null,
                infoMessage = "命令仍在等待设备回执，请确认模拟车机或真实车机是否在线"
            )
        }
    }

    private fun buildVehicleStateSummary(data: WsVehicleStateData): String {
        return "锁:${data.lockStatus ?: "-"}  " +
                "发动机:${data.engineStatus ?: "-"}  " +
                "空调:${data.hvacStatus ?: "-"}  " +
                "车窗:${data.windowStatus ?: "-"}  " +
                "油量:${data.fuelLevel ?: "-"}  " +
                "更新时间:${data.updatedTime ?: "-"}"
    }

    private fun buildVehicleStateSummary(data: VehicleStateResponse): String {
        return "锁:${data.lockStatus ?: "-"}  " +
                "发动机:${data.engineStatus ?: "-"}  " +
                "空调:${data.hvacStatus ?: "-"}  " +
                "车窗:${data.windowStatus ?: "-"}  " +
                "油量:${data.fuelLevel ?: "-"}  " +
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
        private val commandRepository: CommandRepository,
        private val vehicleRepository: VehicleRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ControlViewModel(commandRepository, vehicleRepository) as T
        }
    }
}