package com.example.sweetbean.data.journal

enum class Emotion(val emoji: String, val label: String) {
    VERY_HAPPY("🤩", "Rất vui"),
    HAPPY("😊", "Vui"),
    NEUTRAL("😐", "Bình thường"),
    SAD("😔", "Buồn"),
    VERY_SAD("😭", "Rất buồn");

    companion object {
        fun fromKey(key: String?): Emotion? = entries.firstOrNull { it.name == key }
    }
}

object MediaType {
    const val IMAGE = "IMAGE"
    const val VIDEO = "VIDEO"
}

object JournalLimits {
    const val MAX_TITLE_LENGTH = 100
    const val MAX_TEXT_LENGTH = 2000
    const val MAX_MEDIA = 10
}

/** Phần tử của mảng `media` trong journals/{journalId} */
data class JournalMedia(
    val type: String,
    val url: String,
    val publicId: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val format: String? = null
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO

    /** Cloudinary trả về một khung hình của video khi đổi đuôi URL thành .jpg. */
    val thumbnailUrl: String
        get() = if (isVideo) url.substringBeforeLast('.') + ".jpg" else url
    val playbackUrl: String
        get() = if (isVideo) url.substringBeforeLast('.') + ".mp4" else url
}

data class Journal(
    val id: String,
    val userId: String,
    val dateMillis: Long,
    val title: String,
    val text: String,
    val emotion: Emotion,
    val tags: List<String>,
    val media: List<JournalMedia>
)

/** Dữ liệu người dùng nhập; các trường hệ thống (userId, date, createdAt...) do repository đặt. */
data class JournalDraft(
    val title: String,
    val text: String,
    val emotion: Emotion,
    val tags: List<String>,
    val media: List<JournalMedia>
)