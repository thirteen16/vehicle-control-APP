package com.example.app.data.repository

import com.example.app.common.ResultState
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.model.request.CommandRequest
import com.example.app.data.model.response.CommandHistoryItemResponse
import com.example.app.data.model.response.CommandResultResponse
import com.example.app.data.model.response.CommandSubmitResponse
import com.example.app.data.remote.api.CommandApi

class CommandRepository(
    private val commandApi: CommandApi,
    private val selectedVehicleStore: SelectedVehicleStore
) {

    suspend fun getSelectedVehicleId(): String? {
        return selectedVehicleStore.getSelectedVehicleId()
    }

    suspend fun submitCommand(
        vehicleId: String,
        type: String
    ): ResultState<CommandSubmitResponse> {
        return try {
            val response = commandApi.submitCommand(
                CommandRequest(
                    vehicleId = vehicleId,
                    type = type
                )
            )
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "命令发送失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "命令发送失败")
            )
        }
    }

    suspend fun getCommandResult(commandId: String): ResultState<CommandResultResponse> {
        return try {
            val response = commandApi.getCommandResult(commandId)
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "获取命令结果失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "获取命令结果失败")
            )
        }
    }

    suspend fun getCommandHistory(
        vehicleId: String?,
        limit: Int = 50
    ): ResultState<List<CommandHistoryItemResponse>> {
        return try {
            val response = commandApi.getCommandHistory(vehicleId, limit)
            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "获取命令历史失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "获取命令历史失败")
            )
        }
    }
}