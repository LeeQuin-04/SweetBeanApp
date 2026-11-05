package com.example.sweetbean.domain.auth

sealed interface RegisterResult {

    /** Đăng ký trọn vẹn: tài khoản, hồ sơ, Username và email xác minh đều xong. */
    data object Success : RegisterResult

    /** Thất bại trước khi tạo tài khoản (hoặc không tạo được). Không có gì được lưu. */
    data class Error(val message: String) : RegisterResult

    /** Tài khoản đã được tạo nhưng hồ sơ hoặc Username chưa hoàn tất. Gate sẽ yêu cầu thiết lập tiếp. */
    data class AccountCreatedIncomplete(val message: String) : RegisterResult

    /** Tài khoản và Username đã xong nhưng chưa gửi được email xác minh. */
    data class VerificationEmailFailed(val message: String) : RegisterResult
}
