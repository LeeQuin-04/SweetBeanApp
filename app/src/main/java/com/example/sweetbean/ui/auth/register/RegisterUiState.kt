package com.example.sweetbean.ui.auth.register

data class RegisterUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,

    val errorMessage: String? = null,
    val isRegistrationSuccessful: Boolean = false
)
