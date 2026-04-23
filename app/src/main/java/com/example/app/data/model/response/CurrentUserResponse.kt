package com.example.app.data.model.response

data class CurrentUserResponse(
    val userId: Long,
    val username: String,
    val phone: String?,
    val nickname: String?,
    val role: String?
)