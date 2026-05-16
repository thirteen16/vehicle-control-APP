package com.example.app.data.model.request

data class VehicleBindRequest(
    val vehicleId: String
)

data class VehicleVerifyRequest(
    val requestId: String,
    val code: String
)