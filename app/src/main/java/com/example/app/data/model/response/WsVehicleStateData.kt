package com.example.app.data.model.response

data class WsVehicleStateData(
    val vehicleId: String? = null,
    val name: String? = null,
    val onlineStatus: Int? = null,
    val lockStatus: String? = null,
    val engineStatus: String? = null,
    val hvacStatus: String? = null,
    val windowStatus: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val mileage: Int? = null,
    val fuelLevel: Int? = null,
    val updatedTime: String? = null
)