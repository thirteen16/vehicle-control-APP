package com.example.app.data.remote.api

import com.example.app.data.model.request.LoginRequest
import com.example.app.data.model.request.RegisterRequest
import com.example.app.data.model.request.ResetPasswordRequest
import com.example.app.data.model.request.SendLoginCodeRequest
import com.example.app.data.model.request.SendResetCodeRequest
import com.example.app.data.model.request.SmsLoginRequest
import com.example.app.data.model.request.VerifyPinCodeRequest
import com.example.app.data.model.response.ApiResponse
import com.example.app.data.model.response.CurrentUserResponse
import com.example.app.data.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): ApiResponse<LoginResponse>

    @GET("me")
    suspend fun getCurrentUser(): ApiResponse<CurrentUserResponse>

    @POST("auth/send-login-code")
    suspend fun sendLoginCode(
        @Body request: SendLoginCodeRequest
    ): ApiResponse<Unit>

    @POST("auth/sms-login")
    suspend fun smsLogin(
        @Body request: SmsLoginRequest
    ): ApiResponse<LoginResponse>

    @POST("auth/send-reset-code")
    suspend fun sendResetCode(
        @Body request: SendResetCodeRequest
    ): ApiResponse<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): ApiResponse<Unit>

    @POST("auth/send-pin-code")
    suspend fun sendPinCode(): ApiResponse<Unit>

    @POST("auth/verify-pin-code")
    suspend fun verifyPinCode(
        @Body request: VerifyPinCodeRequest
    ): ApiResponse<Unit>
}
