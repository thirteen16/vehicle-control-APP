package com.example.app.data.repository

import com.example.app.common.ResultState
import com.example.app.data.model.request.ChangePhoneRequest
import com.example.app.data.model.request.UpdateNicknameRequest
import com.example.app.data.remote.api.UserProfileApi

class UserProfileRepository(
    private val userProfileApi: UserProfileApi
) {

    suspend fun updateNickname(nickname: String): ResultState<String> {
        return try {
            val response = userProfileApi.updateNickname(
                UpdateNicknameRequest(nickname = nickname)
            )

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "昵称修改成功" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "昵称修改失败" },
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

    suspend fun changePhone(newPhone: String, code: String): ResultState<String> {
        return try {
            val response = userProfileApi.changePhone(
                ChangePhoneRequest(
                    newPhone = newPhone,
                    code = code
                )
            )

            if (response.code == 200) {
                ResultState.Success(response.message.ifBlank { "手机号换绑成功" })
            } else {
                ResultState.Error(
                    message = response.message.ifBlank { "手机号换绑失败" },
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