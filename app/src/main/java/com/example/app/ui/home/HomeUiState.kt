package com.example.app.ui.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val username: String = "用户",
    val loggedOut: Boolean = false,
    val errorMessage: String? = null
)