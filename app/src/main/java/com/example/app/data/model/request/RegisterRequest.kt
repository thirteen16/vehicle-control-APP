package com.example.app.data.model.request

data class RegisterRequest(
    val username: String,
    val password: String,
    val phone: String,
    val nickname: String
)