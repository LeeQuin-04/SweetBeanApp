package com.example.sweetbean.data.auth

sealed interface AuthResult {
    data object Success : AuthResult

    data class Error(
        val message: String,
        val isRateLimited: Boolean = false
    ) : AuthResult
}
