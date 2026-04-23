package com.example.app.ui.home

import com.example.app.data.model.entity.Vehicle
import com.example.app.data.model.response.VehicleStateResponse

data class HomeUiState(
    val isLoading: Boolean = false,
    val username: String = "用户",
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val selectedState: VehicleStateResponse? = null,
    val emptyMessage: String? = null,
    val loggedOut: Boolean = false,
    val errorMessage: String? = null
)