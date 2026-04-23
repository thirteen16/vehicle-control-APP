package com.example.app.data.remote.api

import com.example.app.data.model.entity.Vehicle
import com.example.app.data.model.response.ApiResponse
import com.example.app.data.model.response.VehicleStateResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface VehicleApi {

    @GET("vehicles")
    suspend fun getVehicles(): ApiResponse<List<Vehicle>>

    @GET("vehicles/{vehicleId}/state")
    suspend fun getVehicleState(
        @Path("vehicleId") vehicleId: String
    ): ApiResponse<VehicleStateResponse>
}