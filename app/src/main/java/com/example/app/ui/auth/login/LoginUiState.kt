package com.example.app.ui.auth.login

import com.example.app.data.model.response.LoginResponse

data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val autoLogin: Boolean = false,
    val loginData: LoginResponse? = null,
    val errorMessage: String? = null,

    val rememberedAccount: String = "",
    val rememberedPassword: String = "",
    val rememberPassword: Boolean = false,
    val autoLoginEnabled: Boolean = false,
    val rememberedInfoLoaded: Boolean = false
)