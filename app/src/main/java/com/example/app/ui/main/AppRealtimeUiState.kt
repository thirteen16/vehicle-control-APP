package com.example.app.ui.main

import com.example.app.data.model.response.WsCommandAckData
import com.example.app.data.model.response.WsVehicleStateData

data class AppRealtimeUiState(
    val isConnecting: Boolean = false,
    val wsConnected: Boolean = false,
    val latestCommandAck: WsCommandAckData? = null,
    val latestVehicleState: WsVehicleStateData? = null,
    val latestVehicleStateText: String? = null,
    val errorMessage: String? = null
)