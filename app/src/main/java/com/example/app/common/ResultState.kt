package com.example.app.common

sealed class ResultState<out T> {
    data object Loading : ResultState<Nothing>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val message: String, val code: Int? = null) : ResultState<Nothing>()
}