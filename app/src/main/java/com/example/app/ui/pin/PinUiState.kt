package com.example.app.ui.pin

data class PinUiState(
    val hasPin: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val verifySuccess: Boolean = false,
    val clearSuccess: Boolean = false
)