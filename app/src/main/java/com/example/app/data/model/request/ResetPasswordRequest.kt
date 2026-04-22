package com.example.app.data.model.request

data class ResetPasswordRequest(
    val phone: String,
    val code: String,
    val newPassword: String,
    val confirmPassword: String
)