package com.example.sweetbean.ui.auth.gate

data class AuthGateUiState(
    /** Chỉ true ở lần kiểm tra đầu tiên khi mở app. Splash chỉ hiện trong lúc này. */
    val isInitializing: Boolean = false,
    /** True khi không có tài khoản nào đăng nhập (vừa đăng xuất hoặc chưa đăng nhập). */
    val isSignedOut: Boolean = false,
    val isCheckingAuth: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isEmailVerificationRequired: Boolean = false,
    val isUsernameSetupRequired: Boolean = false,
    val isSendingVerificationEmail: Boolean = false,
    val email: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
