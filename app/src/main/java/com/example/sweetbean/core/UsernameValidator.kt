package com.example.sweetbean.core


object UsernameValidator {

    const val MAX_LENGTH = 20

    private val VALID_PATTERN = Regex("^[A-Za-z0-9_]{3,20}$")

    /** Username để lưu và hiển thị: bỏ khoảng trắng hai đầu, giữ nguyên chữ hoa/thường. */
    fun clean(raw: String): String = raw.trim()

    /** Khoá kiểm tra trùng (usernameLower): "LeeQuin" và "leequin" là cùng một username. */
    fun key(raw: String): String = raw.trim().lowercase()

    fun isValid(raw: String): Boolean = VALID_PATTERN.matches(clean(raw))

    /** Lọc chữ khi người dùng đang gõ: chữ không dấu, số, dấu gạch dưới, tối đa MAX_LENGTH ký tự. */
    fun sanitizeInput(raw: String): String =
        raw.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }
            .take(MAX_LENGTH)
}