package com.example.app.data.model.response

data class LoginResponse(
    val token: String,
    val tokenType: String,
    val userId: Long,
    val username: String,
    val phone: String?,
    val nickname: String?,
    val role: String?
)