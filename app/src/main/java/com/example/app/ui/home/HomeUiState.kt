package com.example.app.ui.home

import com.example.app.data.model.entity.Vehicle
import com.example.app.data.model.response.VehicleStateResponse

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshingCurrent: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val selectedState: VehicleStateResponse? = null,
    val errorMessage: String? = null
)