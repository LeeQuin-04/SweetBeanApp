package com.example.sweetbean.ui.journal

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.sweetbean.data.journal.DayKey

object JournalFormat {
    private val vi = Locale("vi", "VN")

    fun day(millis: Long): String =
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))

    fun time(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun dayOfMonth(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

    /** "Thứ ba", "Chủ nhật"... */
    fun weekday(millis: Long): String =
        SimpleDateFormat("EEEE", vi).format(Date(millis))
            .replaceFirstChar { it.uppercase(vi) }

    /** "Thứ ba, 27 tháng 10 lúc 18:30" */
    fun full(millis: Long): String =
        SimpleDateFormat("EEEE, d 'tháng' M 'lúc' HH:mm", vi).format(Date(millis))
            .replaceFirstChar { it.uppercase(vi) }

    /** "Thứ ba, 27 tháng 10, 2026" */
    fun dayLabel(day: DayKey): String {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(day.year, day.month - 1, day.day)
        }
        return SimpleDateFormat("EEEE, d 'tháng' M, yyyy", vi).format(calendar.time)
            .replaceFirstChar { it.uppercase(vi) }
    }
}