package com.example.app.data.model.response

data class VehicleLocationResponse(
    val vehicleId: String = "",
    val name: String = "",
    val longitude: Double? = null,
    val latitude: Double? = null,
    val onlineStatus: Int? = null,
    val updatedTime: String? = null
)