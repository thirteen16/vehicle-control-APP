package com.example.app.data.remote.api

import com.example.app.data.model.request.CommandRequest
import com.example.app.data.model.response.ApiResponse
import com.example.app.data.model.response.CommandResultResponse
import com.example.app.data.model.response.CommandSubmitResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CommandApi {

    @POST("commands")
    suspend fun submitCommand(
        @Body request: CommandRequest
    ): ApiResponse<CommandSubmitResponse>

    @GET("commands/{commandId}")
    suspend fun getCommandResult(
        @Path("commandId") commandId: String
    ): ApiResponse<CommandResultResponse>
}