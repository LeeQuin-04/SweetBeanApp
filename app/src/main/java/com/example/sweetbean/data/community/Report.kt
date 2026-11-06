package com.example.sweetbean.data.community

object ReportTarget {
    const val POST = "POST"
    const val COMMENT = "COMMENT"
    const val USER = "USER"
}

object ReportLimits {
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_EVIDENCE = 3
}

enum class ReportReason(val code: String, val label: String) {
    PERSONAL_ATTACK("PERSONAL_ATTACK", "Xúc phạm / công kích cá nhân"),
    HATE_SPEECH("HATE_SPEECH", "Ngôn từ thù ghét"),
    THREAT_VIOLENCE("THREAT_VIOLENCE", "Đe dọa / bạo lực"),
    HARASSMENT_BULLYING("HARASSMENT_BULLYING", "Quấy rối / bắt nạt"),
    PRIVACY_VIOLATION("PRIVACY_VIOLATION", "Xâm phạm quyền riêng tư"),
    SEXUAL_CONTENT("SEXUAL_CONTENT", "Nội dung tình dục không phù hợp"),
    CHILD_EXPLOITATION("CHILD_EXPLOITATION", "Khai thác trẻ em"),
    SELF_HARM("SELF_HARM", "Tự gây hại / tự sát"),
    SPAM_SCAM("SPAM_SCAM", "Spam / lừa đảo"),
    IMPERSONATION("IMPERSONATION", "Mạo danh"),
    COPYRIGHT("COPYRIGHT", "Vi phạm bản quyền"),
    OTHER("OTHER", "Khác")
}