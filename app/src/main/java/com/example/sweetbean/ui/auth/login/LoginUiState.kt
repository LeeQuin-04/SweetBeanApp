package com.example.sweetbean.ui.auth.login

// data class phù hợp để chứa dữ liệu. Hàm copy() giúp tạo trạng thái mới
// mà không phải khai báo lại tất cả thuộc tính.
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)
