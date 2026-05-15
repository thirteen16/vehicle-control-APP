package com.example.app.data.repository

import com.example.app.common.ResultState
import com.example.app.data.local.RememberedLoginInfo
import com.example.app.data.local.TokenStore
import com.example.app.data.model.request.LoginRequest
import com.example.app.data.model.request.RegisterRequest
import com.example.app.data.model.request.ResetPasswordRequest
import com.example.app.data.model.request.SendResetCodeRequest
import com.example.app.data.model.request.VerifyPinCodeRequest
import com.example.app.data.model.response.CurrentUserResponse
import com.example.app.data.model.response.LoginResponse
import com.example.app.data.remote.api.AuthApi
import retrofit2.HttpException

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

    suspend fun disableAutoLogin() {
        tokenStore.setAutoLoginEnabled(false)
    }

    /**
     * 校验本地 token 是否仍然有效。
     *
     * 逻辑：
     * 1. 本地没有 token，直接认为无效；
     * 2. 本地有 token，请求 /me；
     * 3. /me 成功，说明 token 没过期，可以直接进主页；
     * 4. /me 返回 401、403 或业务失败，说明 token 不可用，清除旧 token。
     */
    suspend fun validateSavedLoginSession(): ResultState<CurrentUserResponse> {
        val token = tokenStore.getToken()

        if (token.isNullOrBlank()) {
            return ResultState.Error("本地没有保存登录状态")
        }

        return try {
            val response = authApi.getCurrentUser()

            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                tokenStore.clearLoginSession()

                ResultState.Error(
                    message = response.message.ifBlank { "登录状态已过期" },
                    code = response.code
                )
            }
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                tokenStore.clearLoginSession()

                ResultState.Error(
                    message = "登录状态已过期",
                    code = e.code()
                )
            } else {
                ResultState.Error(
                    message = "自动登录校验失败：HTTP ${e.code()}",
                    code = e.code()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()

            ResultState.Error(
                message = "自动登录校验失败：" + (e.message ?: e.javaClass.simpleName)
            )
        }
    }

    /**
     * token 过期后的自动重新登录。
     *
     * 前提：
     * 1. 用户之前勾选过自动登录；
     * 2. 本地保存了账号；
     * 3. 本地保存了密码。
     *
     * 成功后会保存新的 token。
     */
    suspend fun autoLoginWithSavedCredentials(): ResultState<LoginResponse> {
        val rememberedInfo = tokenStore.getRememberedLoginInfo()

        if (!rememberedInfo.autoLoginEnabled) {
            return ResultState.Error("未开启自动登录")
        }

        if (rememberedInfo.account.isBlank() || rememberedInfo.password.isBlank()) {
            tokenStore.clearLoginSession()
            tokenStore.setAutoLoginEnabled(false)

            return ResultState.Error("自动登录需要保存账号和密码，请手动登录")
        }

        return login(
            account = rememberedInfo.account,
            password = rememberedInfo.password,
            rememberPassword = true,
            autoLoginEnabled = true
        )
    }

    suspend fun login(
        account: String,
        password: String,
        rememberPassword: Boolean,
        autoLoginEnabled: Boolean
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
                    rememberPassword = rememberPassword || autoLoginEnabled,
                    autoLoginEnabled = autoLoginEnabled
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

    suspend fun getCurrentUser(): ResultState<CurrentUserResponse> {
        return try {
            val response = authApi.getCurrentUser()

            if (response.code == 200 && response.data != null) {
                ResultState.Success(response.data)
            } else {
                if (response.code == 401 || response.code == 403) {
                    tokenStore.clearLoginSession()
                }

                ResultState.Error(
                    message = response.message.ifBlank { "获取当前用户失败" },
                    code = response.code
                )
            }
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                tokenStore.clearLoginSession()

                ResultState.Error(
                    message = "登录状态已过期，请重新登录",
                    code = e.code()
                )
            } else {
                ResultState.Error(
                    message = "获取当前用户失败：HTTP ${e.code()}",
                    code = e.code()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()

            ResultState.Error(
                message = e.javaClass.simpleName + ": " + (e.message ?: "获取当前用户失败")
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

    suspend fun sendPinCode(): ResultState<String> {
        return try {
            val response = authApi.sendPinCode()

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "PIN 验证码发送成功" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "PIN 验证码发送失败" },
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

    suspend fun verifyPinCode(code: String): ResultState<String> {
        return try {
            val response = authApi.verifyPinCode(
                VerifyPinCodeRequest(code = code)
            )

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "PIN 验证码校验通过" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "PIN 验证码校验失败" },
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

    /**
     * 用户主动退出登录时，必须关闭自动登录。
     *
     * 否则用户点了退出登录，下次打开 App 又会用保存的账号密码自动登录回来。
     */
    suspend fun logout() {
        tokenStore.clearLoginSession()
        tokenStore.setAutoLoginEnabled(false)
    }
}