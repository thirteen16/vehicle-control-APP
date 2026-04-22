package com.example.app.ui.auth.forget

data class ForgetPasswordUiState(
    val isSendingCode: Boolean = false,
    val isResetting: Boolean = false,
    val countdownSeconds: Int = 0,
    val sendCodeSuccessMessage: String? = null,
    val resetSuccess: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)