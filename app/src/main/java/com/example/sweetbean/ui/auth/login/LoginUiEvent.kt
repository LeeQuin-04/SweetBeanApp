package com.example.sweetbean.ui.auth.login

// sealed interface tạo ra một nhóm sự kiện có giới hạn, xác định rõ những thao tác
// mà màn hình đăng nhập hỗ trợ.
sealed interface LoginUiEvent {

    // Sự kiện mang dữ liệu: khi người dùng nhập email hoặc mật khẩu.
    data class EmailChanged(
        val value: String
    ) : LoginUiEvent

    data class PasswordChanged(
        val value: String
    ) : LoginUiEvent

    // Sự kiện không cần dữ liệu: chỉ biểu thị hành động.
    data object TogglePasswordVisibility : LoginUiEvent

    data object LoginClicked : LoginUiEvent

    data object ErrorDismissed : LoginUiEvent
}
