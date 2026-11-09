package com.example.sweetbean.data.journal

import java.util.Calendar
import java.util.Locale

/** Một ngày lịch theo giờ của máy. [month] tính từ 1 đến 12. */
data class DayKey(val year: Int, val month: Int, val day: Int) {

    /** ID document của nhật ký ngày này: {uid}_{yyyyMMdd}. */
    fun id(uid: String): String =
        uid + "_" + String.format(Locale.US, "%04d%02d%02d", year, month, day)

    /** "27/10/2026" */
    fun label(): String = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)

    private val sortable: Int get() = year * 10_000 + month * 100 + day

    fun isToday(): Boolean = sortable == today().sortable

    fun isFuture(): Boolean = sortable > today().sortable

    /** Thời điểm trong ngày này, giờ phút giây là lúc đang gọi. */
    fun atCurrentTimeOfDay(): Long {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            clear()
            set(
                year, month - 1, day,
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND)
            )
        }.timeInMillis
    }

    companion object {
        fun today(): DayKey = of(System.currentTimeMillis())

        fun of(millis: Long): DayKey {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return DayKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }
    }
}