package com.example.sweetbean.ui.journal.tree

import com.example.sweetbean.data.journal.Emotion
import com.example.sweetbean.data.journal.Journal
import java.util.Calendar
import kotlin.math.abs

enum class MonthStatus { FUTURE, EMPTY, IN_PROGRESS, DONE }

data class MonthSummary(
    val month: Int,                    // 1..12
    val status: MonthStatus,
    val counts: Map<Emotion, Int>,     // chỉ gồm cảm xúc có xuất hiện
    val dominant: List<Emotion>        // 1 cảm xúc chủ đạo, hoặc nhiều nếu hòa nhau
) {
    val total: Int get() = counts.values.sum()
}

object EmotionTreeCalculator {

    fun summarize(
        year: Int,
        journals: List<Journal>,
        now: Calendar = Calendar.getInstance()
    ): List<MonthSummary> {
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1

        val byMonth = journals.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.MONTH) + 1
        }

        return (1..12).map { month ->
            val list = byMonth[month].orEmpty()
            val counts = Emotion.entries
                .associateWith { emotion -> list.count { it.emotion == emotion } }
                .filterValues { it > 0 }

            val max = counts.values.maxOrNull()
            // Hòa nhau: giữ cả các cảm xúc bằng số lượng cao nhất, màu sẽ được pha trộn.
            val dominant = if (max == null) emptyList() else counts.filterValues { it == max }.keys.toList()

            val isFuture = year > currentYear || (year == currentYear && month > currentMonth)
            val isCurrent = year == currentYear && month == currentMonth

            val status = when {
                isFuture -> MonthStatus.FUTURE
                list.isEmpty() -> MonthStatus.EMPTY
                isCurrent -> MonthStatus.IN_PROGRESS
                else -> MonthStatus.DONE
            }

            MonthSummary(month, status, counts, dominant)
        }
    }
}

