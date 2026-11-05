package com.example.sweetbean.ui.auth.forgotpassword

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isResetEmailSent: Boolean = false
)
