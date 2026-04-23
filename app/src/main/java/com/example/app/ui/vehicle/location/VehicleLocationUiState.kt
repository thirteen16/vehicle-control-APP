package com.example.app.ui.vehicle.location

import com.example.app.data.model.response.VehicleLocationResponse

data class VehicleLocationUiState(
    val isLoading: Boolean = false,
    val vehicleId: String = "",
    val location: VehicleLocationResponse? = null,
    val errorMessage: String? = null
)