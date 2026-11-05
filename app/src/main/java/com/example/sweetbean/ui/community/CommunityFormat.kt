package com.example.sweetbean.ui.community

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommunityFormat {

    fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - millis).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "Vừa xong"
            diff < hour -> "${diff / minute} phút trước"
            diff < day -> "${diff / hour} giờ trước"
            diff < 7 * day -> "${diff / day} ngày trước"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(millis))
        }
    }
}