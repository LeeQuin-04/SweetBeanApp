package com.example.sweetbean.ui.journal.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sweetbean.data.journal.Emotion
import com.example.sweetbean.data.journal.Journal
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.journal.JournalFormat
import com.example.sweetbean.ui.journal.components.EmotionBadge
import com.example.sweetbean.ui.journal.components.EmotionIcon
import com.example.sweetbean.ui.journal.components.JournalBackdrop
import com.example.sweetbean.ui.journal.components.MediaThumb
import com.example.sweetbean.ui.journal.components.accent
import com.example.sweetbean.ui.journal.components.bouncyClickable
import java.util.Calendar
import com.example.sweetbean.ui.journal.components.JournalScene
import com.example.sweetbean.ui.journal.emotion.EmotionSetButton

@Composable
fun JournalListScreen(
    uiState: JournalListUiState,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    onJournalClick: (Journal) -> Unit,
    onCreateClick: () -> Unit
) {
    val barHeight = LocalFloatingBarHeight.current
    val now = remember { Calendar.getInstance() }
    val currentYear = now.get(Calendar.YEAR)
    val today = now.get(Calendar.DAY_OF_MONTH)
    val isCurrentMonth =
        uiState.year == currentYear && uiState.month == now.get(Calendar.MONTH) + 1

    val monthOptions = remember { (1..12).map { it to "Tháng $it" } }
    val yearOptions = remember(currentYear) {
        (currentYear downTo currentYear - 5).map { it to it.toString() }
    }

    // Nhật ký mới nhất của từng ngày trong tháng, dùng cho dải cảm xúc.
    val latestByDay: Map<Int, Journal> = remember(uiState.groups) {
        val map = LinkedHashMap<Int, Journal>()
        uiState.groups.forEach { group ->
            group.items.forEach { journal ->
                map.getOrPut(JournalFormat.dayOfMonth(journal.dateMillis)) { journal }
            }
        }
        map
    }
    val daysInMonth = remember(uiState.year, uiState.month) {
        Calendar.getInstance().apply {
            clear()
            set(uiState.year, uiState.month - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val hasToday = isCurrentMonth && latestByDay.containsKey(today)

    JournalBackdrop(scene = JournalScene.List) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp + barHeight)
        ) {
            item {
                ListHeader(
                    uiState = uiState,
                    daysWritten = latestByDay.size,
                    daysInMonth = daysInMonth,
                    monthOptions = monthOptions,
                    yearOptions = yearOptions,
                    onMonthSelected = onMonthSelected,
                    onYearSelected = onYearSelected
                )
            }

            when {
                uiState.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.groups.isEmpty() -> item { EmptyState() }

                else -> itemsIndexed(
                    items = uiState.groups,
                    key = { _, group -> group.label }
                ) { index, group ->
                    TimelineDay(
                        group = group,
                        isLast = index == uiState.groups.lastIndex,
                        onJournalClick = onJournalClick
                    )
                }
            }
        }

        WriteButton(
            hasToday = hasToday,
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp + barHeight)
        )
    }
}

@Composable
private fun ListHeader(
    uiState: JournalListUiState,
    daysWritten: Int,
    daysInMonth: Int,
    monthOptions: List<Pair<Int, String>>,
    yearOptions: List<Pair<Int, String>>,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
        Text(
            text = "Nhật ký",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = when {
                uiState.isLoading -> "Đang tải nhật ký…"
                daysWritten == 0 -> "Chưa có ngày nào được ghi"
                else -> "Đã ghi $daysWritten/$daysInMonth ngày"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            PeriodChip("Tháng ${uiState.month}", monthOptions, onMonthSelected)
            PeriodChip(uiState.year.toString(), yearOptions, onYearSelected)
            EmotionSetButton()
        }
    }
}

@Composable
private fun PeriodChip(
    text: String,
    options: List<Pair<Int, String>>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Box {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.12f))
                .clickable { expanded = true }
                .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, fontWeight = FontWeight.SemiBold, color = primary)
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}


/** Một ngày trên dòng thời gian: nút tròn mang màu cảm xúc, đường ray dọc nối các ngày. */
@Composable
private fun TimelineDay(
    group: JournalDayGroup,
    isLast: Boolean,
    onJournalClick: (Journal) -> Unit
) {
    val first = group.items.first()
    val accent = first.emotion.accent
    val rail = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .drawBehind {
                if (!isLast) {
                    val x = 14.dp.toPx()
                    drawLine(
                        color = rail,
                        start = Offset(x, 38.dp.toPx()),
                        end = Offset(x, size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                EmotionIcon(emotion = first.emotion, size = 22.dp)
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = JournalFormat.dayOfMonth(first.dateMillis).toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = JournalFormat.weekday(first.dateMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                group.items.forEach { journal ->
                    key(journal.id) {
                        JournalCard(journal = journal, onClick = { onJournalClick(journal) })
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalCard(
    journal: Journal,
    onClick: () -> Unit
) {
    val accent = journal.emotion.accent
    val surface = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.22f).compositeOver(surface), surface)
                )
            )
            .border(1.dp, accent.copy(alpha = 0.28f), shape)
            .padding(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmotionBadge(emotion = journal.emotion, size = 48.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = journal.title.ifBlank { journal.emotion.label },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = JournalFormat.time(journal.dateMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (journal.text.isNotBlank()) {
                    Text(
                        text = journal.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (journal.media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            MediaPreviewRow(journal)
        }
    }
}

/** Tối đa 3 ảnh thu nhỏ; nếu còn nhiều hơn thì ô thứ 3 hiện "+N". */
@Composable
private fun MediaPreviewRow(journal: Journal) {
    val shown = journal.media.take(3)
    val extra = journal.media.size - shown.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        shown.forEachIndexed { index, media ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                MediaThumb(
                    model = media.thumbnailUrl,
                    isVideo = media.isVideo,
                    modifier = Modifier.fillMaxSize()
                )
                if (index == 2 && extra > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+$extra", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        repeat(3 - shown.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun EmptyState() {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.30f), primary.copy(alpha = 0.06f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            EmotionIcon(emotion = Emotion.HAPPY, size = 84.dp)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Tháng này chưa có trang nào",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Viết vài dòng về hôm nay để bắt đầu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WriteButton(
    hasToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val shape = RoundedCornerShape(28.dp)
    val content = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = modifier
            .bouncyClickable(onClick)
            .shadow(14.dp, shape, ambientColor = primary, spotColor = primary)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(primary, lerp(primary, tertiary, 0.55f))))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (hasToday) Icons.Default.Edit else Icons.Default.Add,
            contentDescription = null,
            tint = content
        )
    }
}