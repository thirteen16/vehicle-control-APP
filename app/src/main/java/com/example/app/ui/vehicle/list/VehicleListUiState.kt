package com.example.app.ui.vehicle.list

import com.example.app.data.model.entity.Vehicle

data class VehicleListUiState(
    val isLoading: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)