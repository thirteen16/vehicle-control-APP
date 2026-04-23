package com.example.app.data.model.entity

data class Vehicle(
    val id: Long? = null,
    val vehicleId: String = "",
    val name: String = "",
    val brand: String? = null,
    val model: String? = null,
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