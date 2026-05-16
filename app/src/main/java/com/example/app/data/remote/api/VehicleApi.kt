package com.example.app.data.remote.api

import com.example.app.data.model.entity.Vehicle
import com.example.app.data.model.request.VehicleBindRequest
import com.example.app.data.model.request.VehicleVerifyRequest
import com.example.app.data.model.response.ApiResponse
import com.example.app.data.model.response.VehicleBindResponse
import com.example.app.data.model.response.VehicleLocationResponse
import com.example.app.data.model.response.VehicleStateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VehicleApi {

    @GET("vehicles")
    suspend fun getVehicles(): ApiResponse<List<Vehicle>>

    @GET("vehicles/{vehicleId}/state")
    suspend fun getVehicleState(
        @Path("vehicleId") vehicleId: String
    ): ApiResponse<VehicleStateResponse>

    @GET("vehicles/{vehicleId}/location")
    suspend fun getVehicleLocation(
        @Path("vehicleId") vehicleId: String
    ): ApiResponse<VehicleLocationResponse>

    @POST("vehicles/bind/request")
    suspend fun requestBindVehicle(
        @Body request: VehicleBindRequest
    ): ApiResponse<VehicleBindResponse>

    @POST("vehicles/bind/verify")
    suspend fun verifyBindVehicle(
        @Body request: VehicleVerifyRequest
    ): ApiResponse<VehicleBindResponse>

    @POST("vehicles/unbind/request")
    suspend fun requestUnbindVehicle(
        @Body request: VehicleBindRequest
    ): ApiResponse<VehicleBindResponse>

    @POST("vehicles/unbind/verify")
    suspend fun verifyUnbindVehicle(
        @Body request: VehicleVerifyRequest
    ): ApiResponse<VehicleBindResponse>
}