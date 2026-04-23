package com.example.app.ui.control

data class ControlUiState(
    val isLoading: Boolean = false,
    val isPolling: Boolean = false,

    val selectedVehicleId: String? = null,
    val wsConnected: Boolean = false,

    val onlineStatus: Int? = null,
    val lockStatus: String? = null,
    val engineStatus: String? = null,
    val hvacStatus: String? = null,
    val windowStatus: String? = null,

    val latestVehicleStateText: String? = null,

    val pendingCommandType: String? = null,

    val lastCommandId: String? = null,
    val lastCommandType: String? = null,
    val lastCommandResult: String? = null,
    val lastRequestTime: String? = null,
    val lastResponseTime: String? = null,

    val infoMessage: String? = null,
    val errorMessage: String? = null
)