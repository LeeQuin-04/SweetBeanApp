package com.example.sweetbean.core

object AuthMessages {

    // Dùng chung
    const val ENTER_EMAIL = "Vui lòng nhập email."
    const val ENTER_PASSWORD = "Vui lòng nhập mật khẩu."
    const val ENTER_EMAIL_AND_PASSWORD = "Vui lòng nhập email và mật khẩu."
    const val EMAIL_INVALID = "Email không đúng định dạng."
    const val GENERIC_ERROR = "Đã xảy ra lỗi. Vui lòng thử lại."
    const val SESSION_EXPIRED = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."

    // Đăng nhập
    const val ACCOUNT_NOT_FOUND = "Không thể xác định tài khoản."
    const val EMAIL_NOT_VERIFIED = "Email chưa được xác minh. Vui lòng kiểm tra hộp thư."
    const val LOGIN_FAILED = "Đăng nhập thất bại. Vui lòng kiểm tra thông tin và thử lại."

    // Đăng ký
    const val ENTER_ALL_REGISTER_FIELDS = "Vui lòng nhập đầy đủ thông tin đăng ký."
    const val ENTER_USERNAME = "Vui lòng nhập username."
    const val USERNAME_INVALID =
        "Username phải dài 3–20 ký tự, chỉ gồm chữ cái không dấu, số và dấu gạch dưới."
    const val PASSWORD_TOO_SHORT = "Mật khẩu phải có ít nhất 6 ký tự."
    const val ENTER_CONFIRM_PASSWORD = "Vui lòng xác nhận mật khẩu."
    const val PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp."
    const val USERNAME_TAKEN = "Username này đã được sử dụng. Vui lòng chọn Username khác."
    const val USERNAME_CHECK_FAILED = "Không thể kiểm tra Username. Vui lòng thử lại."
    const val CREATE_ACCOUNT_FAILED = "Không thể tạo tài khoản. Vui lòng kiểm tra thông tin."
    const val EMAIL_ALREADY_IN_USE =
        "Email này đã được sử dụng. Nếu bạn từng đăng nhập bằng Google, hãy dùng nút Đăng nhập bằng Google."
    const val REGISTER_FAILED = "Đăng ký thất bại. Vui lòng thử lại."

    // Đăng ký: tài khoản đã được tạo nhưng chưa hoàn tất
    const val PROFILE_NOT_SAVED_AFTER_CREATE =
        "Tài khoản đã được tạo nhưng chưa lưu được hồ sơ. Vui lòng thử lại."
    const val USERNAME_NOT_SAVED_AFTER_CREATE =
        "Tài khoản đã được tạo nhưng chưa lưu được Username. Vui lòng thử lại."
    const val USERNAME_TAKEN_AFTER_CREATE =
        "Username vừa được người khác sử dụng. Tài khoản đã được tạo; cần hoàn tất thiết lập Username trước khi tiếp tục."
    const val PROFILE_INCOMPLETE_AFTER_CREATE =
        "Tài khoản đã được tạo nhưng chưa hoàn tất hồ sơ. Vui lòng thử lại."
    const val VERIFICATION_EMAIL_FAILED_AFTER_CREATE =
        "Tài khoản đã được tạo và Username đã lưu, nhưng chưa gửi được email xác minh. Vui lòng gửi lại email xác minh."

    // Xác minh email
    const val NOT_LOGGED_IN = "Bạn chưa đăng nhập."
    const val SESSION_NOT_FOUND = "Không tìm thấy phiên đăng nhập."
    const val VERIFICATION_SEND_FAILED = "Không thể gửi email xác minh. Vui lòng thử lại."
    const val ACCOUNT_REFRESH_FAILED = "Không thể cập nhật trạng thái tài khoản."
    const val EMAIL_CHECK_FAILED = "Không thể kiểm tra email. Vui lòng thử lại."
    const val VERIFICATION_RESENT = "Đã gửi lại email xác minh. Hãy kiểm tra hộp thư (cả thư mục Spam)."
    const val TOO_MANY_REQUESTS = "Vui lòng đợi vài phút rồi thử lại."
    // Quên mật khẩu
    const val RESET_EMAIL_FAILED = "Không thể gửi email đặt lại mật khẩu."
    const val RESET_REQUEST_FAILED = "Không thể gửi yêu cầu. Vui lòng thử lại."
    const val RESET_EMAIL_SENT = "Nếu email hợp lệ, hãy kiểm tra hộp thư để đặt lại mật khẩu."

    // Thiết lập Username
    const val SETUP_USERNAME_TAKEN = "Username này đã có người sử dụng. Hãy chọn tên khác."
    const val PROFILE_NOT_FOUND = "Không tìm thấy hồ sơ người dùng. Vui lòng thử đăng nhập lại."
    const val USERNAME_SAVE_FAILED = "Không thể lưu Username. Vui lòng thử lại."

    // Cổng xác thực
    const val PROFILE_RECOVER_FAILED = "Chưa thể khôi phục hồ sơ. Vui lòng thử lại."
    const val PROFILE_CHECK_FAILED = "Không thể kiểm tra hồ sơ. Vui lòng thử lại."
    const val LOGOUT_FAILED = "Đăng xuất thất bại. Vui lòng thử lại."

    // Google
    const val GOOGLE_NO_TOKEN = "Không nhận được thông tin xác thực từ Google."
    const val GOOGLE_ACCOUNT_NOT_FOUND = "Không thể xác định tài khoản Google."
    const val GOOGLE_SIGN_IN_FAILED = "Đăng nhập Google thất bại. Vui lòng thử lại."
    const val GOOGLE_LAUNCH_FAILED = "Không thể đăng nhập Google. Vui lòng thử lại."
    const val GOOGLE_UNEXPECTED_ERROR = "Đã xảy ra lỗi khi đăng nhập Google."
}
