package com.example.app.ui.control

data class ControlUiState(
    val isLoading: Boolean = false,
    val isPolling: Boolean = false,
    val wsConnected: Boolean = false,
    val selectedVehicleId: String? = null,
    val lastCommandId: String? = null,
    val lastCommandType: String? = null,
    val lastCommandResult: String? = null,
    val lastRequestTime: String? = null,
    val lastResponseTime: String? = null,
    val latestVehicleStateText: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)