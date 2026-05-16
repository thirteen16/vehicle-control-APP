package com.example.app.data.model.response

data class VehicleBindResponse(
    val id: Long? = null,
    val requestId: String? = null,
    val userId: Long? = null,
    val vehiclePkId: Long? = null,
    val vehicleId: String? = null,
    val action: String? = null,
    val actionText: String? = null,
    val status: String? = null,
    val statusText: String? = null,
    val verifyCode: String? = null,
    val expireTime: String? = null,
    val createdTime: String? = null,
    val updatedTime: String? = null
)