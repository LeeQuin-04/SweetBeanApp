package com.example.sweetbean.ui.journal.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.journal.DayKey
import com.example.sweetbean.data.journal.Journal
import com.example.sweetbean.ui.journal.components.EmotionBadge
import com.example.sweetbean.ui.journal.components.bouncyClickable
import java.util.Calendar

private val WeekdayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
private const val CELL_RATIO = 0.60f   // rộng / cao của một ô ngày

/**
 * Lịch của một tháng. [journalsByDay] ánh xạ ngày trong tháng (1..31) tới nhật ký của ngày đó.
 * Chạm ô ngày nào cũng gọi [onDayClick]; ngày tương lai bị mờ và không chạm được.
 */
@Composable
fun JournalCalendar(
    year: Int,
    month: Int,
    journalsByDay: Map<Int, Journal>,
    onDayClick: (DayKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { DayKey.today() }

    val first = remember(year, month) {
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }
    }
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Thứ hai là cột đầu; Calendar.DAY_OF_WEEK có Chủ nhật = 1 đến Thứ bảy = 7.
    val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val rows = (offset + daysInMonth + 6) / 7

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                WeekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 6.dp)
                    )
                }
            }

            repeat(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val day = row * 7 + col - offset + 1
                        if (day in 1..daysInMonth) {
                            val key = DayKey(year, month, day)
                            DayCell(
                                day = day,
                                journal = journalsByDay[day],
                                isToday = key == today,
                                isFuture = key.isFuture(),
                                onClick = { onDayClick(key) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f).aspectRatio(CELL_RATIO))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    journal: Journal?,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val base = modifier
        .aspectRatio(CELL_RATIO)
        .padding(2.dp)
    val interactive = if (isFuture) base else base.bouncyClickable(onClick)

    val description = when {
        isFuture -> "Ngày $day, chưa tới"
        journal != null -> "Ngày $day, ${journal.emotion.label}"
        else -> "Ngày $day, chưa có nhật ký, chạm để viết"
    }

    Column(
        modifier = interactive
            .clip(RoundedCornerShape(14.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (!isFuture) role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isToday) colors.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),

                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> colors.onPrimary
                    isFuture -> colors.onSurface.copy(alpha = 0.3f)
                    else -> colors.onSurface
                }
            )
        }

        Spacer(Modifier.size(2.dp))

        when {
            journal != null -> EmotionBadge(emotion = journal.emotion, size = 50.dp)

            // Ngày trống ở quá khứ hoặc hôm nay: vòng mờ có dấu cộng gợi ý chạm để viết.
            !isFuture -> Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = colors.onSurface.copy(alpha = 0.28f)
                )
            }

            else -> Spacer(Modifier.size(34.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JournalCalendarPreview(){
    JournalCalendar(
        year = 2026,
        month = 11,
        journalsByDay = emptyMap(),
        {}
    )
}