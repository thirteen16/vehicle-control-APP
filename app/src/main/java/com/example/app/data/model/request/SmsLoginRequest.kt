package com.example.app.data.model.request

data class SmsLoginRequest(
    val phone: String,
    val code: String
)
