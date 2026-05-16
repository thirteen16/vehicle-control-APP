package com.example.app.data.repository

import com.example.app.common.ResultState
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.model.entity.Vehicle
import com.example.app.data.model.request.VehicleBindRequest
import com.example.app.data.model.request.VehicleVerifyRequest
import com.example.app.data.model.response.VehicleBindResponse
import com.example.app.data.model.response.VehicleLocationResponse
import com.example.app.data.model.response.VehicleStateResponse
import com.example.app.data.remote.api.VehicleApi

class VehicleRepository(
    private val vehicleApi: VehicleApi,
    private val selectedVehicleStore: SelectedVehicleStore
) {

    suspend fun getVehicles(): ResultState<List<Vehicle>> {
        return try {
            val response = vehicleApi.getVehicles()
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "获取车辆列表失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "获取车辆列表失败")
            )
        }
    }

    suspend fun getVehicleState(vehicleId: String): ResultState<VehicleStateResponse> {
        return try {
            val response = vehicleApi.getVehicleState(vehicleId)
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "获取车辆状态失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "获取车辆状态失败")
            )
        }
    }

    suspend fun getVehicleLocation(vehicleId: String): ResultState<VehicleLocationResponse> {
        return try {
            val response = vehicleApi.getVehicleLocation(vehicleId)
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "获取车辆定位失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "获取车辆定位失败")
            )
        }
    }

    suspend fun requestBindVehicle(vehicleId: String): ResultState<VehicleBindResponse> {
        return try {
            val response = vehicleApi.requestBindVehicle(VehicleBindRequest(vehicleId))
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "车辆绑定申请失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(e.javaClass.simpleName + ": " + (e.message ?: "车辆绑定申请失败"))
        }
    }

    suspend fun verifyBindVehicle(requestId: String, code: String): ResultState<VehicleBindResponse> {
        return try {
            val response = vehicleApi.verifyBindVehicle(VehicleVerifyRequest(requestId, code))
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "绑定验证码校验失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(e.javaClass.simpleName + ": " + (e.message ?: "绑定验证码校验失败"))
        }
    }

    suspend fun requestUnbindVehicle(vehicleId: String): ResultState<VehicleBindResponse> {
        return try {
            val response = vehicleApi.requestUnbindVehicle(VehicleBindRequest(vehicleId))
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "车辆解绑申请失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(e.javaClass.simpleName + ": " + (e.message ?: "车辆解绑申请失败"))
        }
    }

    suspend fun verifyUnbindVehicle(requestId: String, code: String): ResultState<VehicleBindResponse> {
        return try {
            val response = vehicleApi.verifyUnbindVehicle(VehicleVerifyRequest(requestId, code))
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "解绑验证码校验失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(e.javaClass.simpleName + ": " + (e.message ?: "解绑验证码校验失败"))
        }
    }

    suspend fun saveSelectedVehicleId(vehicleId: String) {
        selectedVehicleStore.saveSelectedVehicleId(vehicleId)
    }

    suspend fun getSelectedVehicleId(): String? {
        return selectedVehicleStore.getSelectedVehicleId()
    }

    suspend fun clearSelectedVehicle() {
        selectedVehicleStore.clear()
    }
}