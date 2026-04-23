package com.example.app.data.remote.api

import com.example.app.data.model.request.ChangePhoneRequest
import com.example.app.data.model.request.SendChangePhoneCodeRequest
import com.example.app.data.model.request.UpdateNicknameRequest
import com.example.app.data.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface UserProfileApi {

    @POST("user/update-nickname")
    suspend fun updateNickname(
        @Body request: UpdateNicknameRequest
    ): ApiResponse<Any>

    @POST("user/send-change-phone-code")
    suspend fun sendChangePhoneCode(
        @Body request: SendChangePhoneCodeRequest
    ): ApiResponse<Any>

    @POST("user/change-phone")
    suspend fun changePhone(
        @Body request: ChangePhoneRequest
    ): ApiResponse<Any>
}