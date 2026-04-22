package com.example.app.ui.auth.register

data class RegisterUiState(
    val isLoading: Boolean = false,
    val registerSuccess: Boolean = false,
    val successMessage: String? = null,
    val registeredAccount: String? = null,
    val errorMessage: String? = null
)