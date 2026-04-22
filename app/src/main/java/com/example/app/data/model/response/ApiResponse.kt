package com.example.app.data.model.response

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)