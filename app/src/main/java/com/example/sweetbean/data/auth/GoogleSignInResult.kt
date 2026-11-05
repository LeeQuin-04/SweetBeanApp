package com.example.sweetbean.data.auth

sealed interface GoogleSignInResult {

    data object Success : GoogleSignInResult

    data object NeedsUsername : GoogleSignInResult

    data class Error(
        val message: String
    ) : GoogleSignInResult
}
