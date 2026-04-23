package com.example.app.ui.vehicle.detail

import com.example.app.data.model.response.VehicleStateResponse

data class VehicleDetailUiState(
    val isLoading: Boolean = false,
    val vehicleId: String = "",
    val vehicleState: VehicleStateResponse? = null,
    val isCurrentVehicle: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)