package com.example.app.data.repository

import com.example.app.common.ResultState
import com.example.app.data.local.RememberedLoginInfo
import com.example.app.data.local.TokenStore
import com.example.app.data.model.request.LoginRequest
import com.example.app.data.model.request.RegisterRequest
import com.example.app.data.model.request.ResetPasswordRequest
import com.example.app.data.model.request.SendResetCodeRequest
import com.example.app.data.model.response.LoginResponse
import com.example.app.data.remote.api.AuthApi

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) {

    suspend fun hasSavedToken(): Boolean {
        return !tokenStore.getToken().isNullOrBlank()
    }

    suspend fun getRememberedLoginInfo(): RememberedLoginInfo {
        return tokenStore.getRememberedLoginInfo()
    }

    suspend fun login(
        account: String,
        password: String,
        rememberPassword: Boolean
    ): ResultState<LoginResponse> {
        return try {
            val response = authApi.login(
                LoginRequest(
                    username = account,
                    password = password
                )
            )

            if (response.code == 200 && response.data != null) {
                tokenStore.saveLoginSession(
                    token = response.data.token,
                    userId = response.data.userId,
                    username = response.data.username ?: account
                )

                tokenStore.saveRememberedLoginInfo(
                    account = account,
                    password = password,
                    rememberPassword = rememberPassword
                )

                ResultState.Success(response.data)
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "登录失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "网络请求失败")
            )
        }
    }

    suspend fun register(
        username: String,
        password: String,
        phone: String,
        nickname: String
    ): ResultState<String> {
        return try {
            val response = authApi.register(
                RegisterRequest(
                    username = username,
                    password = password,
                    phone = phone,
                    nickname = nickname
                )
            )

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "注册成功" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "注册失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "网络请求失败")
            )
        }
    }

    suspend fun sendResetCode(phone: String): ResultState<String> {
        return try {
            val response = authApi.sendResetCode(
                SendResetCodeRequest(phone = phone)
            )

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "验证码发送成功" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "验证码发送失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "网络请求失败")
            )
        }
    }

    suspend fun resetPassword(
        phone: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): ResultState<String> {
        return try {
            val response = authApi.resetPassword(
                ResetPasswordRequest(
                    phone = phone,
                    code = code,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            )

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "密码重置成功" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "密码重置失败" },
                    code = response.code
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "网络请求失败")
            )
        }
    }
}