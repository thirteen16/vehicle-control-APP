package com.example.app.ui.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userId: Long? = null,
    val username: String = "",
    val nickname: String = "",
    val phone: String = "",
    val role: String = "",
    val selectedVehicleId: String = "",
    val selectedVehicleName: String = "",
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val loggedOut: Boolean = false
)