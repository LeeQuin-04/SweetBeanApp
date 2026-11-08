package com.example.sweetbean.ui.journal.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import com.example.sweetbean.data.journal.Emotion
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.journal.components.EmotionIcon
import com.example.sweetbean.ui.journal.components.accent
import java.util.Calendar
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext

private const val MAX_YEARS_BACK = 5

/** Nút nhỏ ở đầu danh sách Nhật ký để mở Cây cảm xúc. */
@Composable
fun EmotionTreeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Mở cây cảm xúc" },
        contentAlignment = Alignment.Center
    ) {
        Text("🌳", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun EmotionTreeScreen(
    uiState: EmotionTreeUiState,
    onBack: () -> Unit,
    onYearChange: (Int) -> Unit,
    onMonthSelect: (Int?) -> Unit
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    // Mỗi lần dữ liệu tải xong, các tháng lần lượt nở màu từ trái sang phải.
    val bloom = remember { Animatable(0f) }
    LaunchedEffect(uiState.year, uiState.isLoading) {
        if (!uiState.isLoading) {
            bloom.snapTo(0f)
            bloom.animateTo(1f, tween(2200, easing = LinearEasing))
        }
    }

    // Chưa có dữ liệu thì vẫn vẽ hình cây mờ để người dùng thấy "cây đang chờ".
    val months = uiState.months.ifEmpty {
        (1..12).map { MonthSummary(it, MonthStatus.FUTURE, emptyMap(), emptyList()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = LocalFloatingBarHeight.current)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Cây cảm xúc",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            YearSwitcher(
                year = uiState.year,
                canGoBack = uiState.year > currentYear - MAX_YEARS_BACK,
                canGoForward = uiState.year < currentYear,
                onYearChange = onYearChange
            )

            Text(
                text = when {
                    uiState.isLoading -> "Đang dựng cây…"
                    uiState.bloomedMonths == 0 -> "Cây sẽ nở màu khi bạn viết nhật ký."
                    else -> "${uiState.bloomedMonths} tháng đã nở màu trong năm ${uiState.year}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            EmotionTreeCanvas(
                months = months,
                bloom = { bloom.value },
                selectedMonth = uiState.selectedMonth,
                onMonthTap = onMonthSelect,
                modifier = Modifier.semantics {
                    contentDescription =
                        "Cây cảm xúc năm ${uiState.year}, ${uiState.bloomedMonths} tháng đã có màu"
                }
            )

            MonthChips(
                months = months,
                selected = uiState.selectedMonth,
                onSelect = onMonthSelect
            )

            val selected = months.firstOrNull { it.month == uiState.selectedMonth }
            if (selected != null) {
                MonthCard(
                    summary = selected
                )
            }

            Legend()

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun YearSwitcher(
    year: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onYearChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { onYearChange(year - 1) }, enabled = canGoBack) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Năm trước")
        }
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        IconButton(onClick = { onYearChange(year + 1) }, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Năm sau")
        }
    }
}

/**
 * Vẽ cây: tô màu vào lưới nhỏ theo dữ liệu, phóng lên rồi cắt theo hình tán lá (SrcIn), sau đó chồng thân cây, bóng cành nhánh và mảng sáng.
 */
@Composable
private fun EmotionTreeCanvas(
    months: List<MonthSummary>,
    bloom: () -> Float,
    selectedMonth: Int?,
    onMonthTap: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val field = remember { TreeField.load(context.resources) }
    val fieldImage = remember(field) { field.bitmap.asImageBitmap() }
    val layerPaint = remember { Paint() }

    val canopyImage = ImageBitmap.imageResource(R.drawable.tree_canopy)
    val baseImage = ImageBitmap.imageResource(R.drawable.tree_base)
    val inkImage = ImageBitmap.imageResource(R.drawable.tree_ink)
    val highlightImage = ImageBitmap.imageResource(R.drawable.tree_highlight)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = if (isDark) Color(0xFF968CBE) else Color(0xFF9682AF)
    val ghostColor = if (isDark) Color(0xFF96A0C8) else Color(0xFFAFACC3)

    // Tháng đang viết dở "thở" nhẹ; chỉ chạy hoạt ảnh khi thật sự có tháng như vậy.
    val hasInProgress = months.any { it.status == MonthStatus.IN_PROGRESS }
    val pulseState = if (hasInProgress) {
        rememberInfiniteTransition(label = "treePulse").animateFloat(
            initialValue = 0f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        null
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(TreeField.IMAGE_RATIO)
            .pointerInput(field) {
                detectTapGestures { offset ->
                    onMonthTap(field.monthAt(offset.x / size.width, offset.y / size.height))
                }
            }
    ) {
        val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())

        // 1) Màu theo dữ liệu vào lưới nhỏ
        field.render(
            buildInks(
                months = months,
                progress = bloom(),
                selectedMonth = selectedMonth,
                pulse = pulseState?.value ?: 0f,
                ghostColor = ghostColor
            )
        )

        // 2) Thân và cỏ
        drawImage(
            image = baseImage,
            dstSize = dst,
            alpha = 0.95f,
            colorFilter = ColorFilter.tint(baseColor, BlendMode.SrcIn),
            filterQuality = FilterQuality.Medium
        )

        // 3) Tán lá: màu (phóng lên mượt) cắt theo hình tán. Chế độ sáng tô hai lớp cho đậm bằng chế độ tối.
        repeat(if (isDark) 1 else 2) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(0f, 0f, size.width, size.height), layerPaint)
                drawImage(
                    image = canopyImage,
                    dstSize = dst,
                    filterQuality = FilterQuality.Medium
                )
                drawImage(
                    image = fieldImage,
                    dstSize = dst,
                    blendMode = BlendMode.SrcIn
                )
                canvas.restore()
            }
        }

        // 4) Cành nhánh, nếp vỏ, bóng và mảng sáng của lá
        drawImage(image = inkImage, dstSize = dst, filterQuality = FilterQuality.Medium)
        drawImage(
            image = highlightImage,
            dstSize = dst,
            alpha = if (isDark) 0.9f else 0.45f,
            filterQuality = FilterQuality.Medium
        )
    }
}

private fun buildInks(
    months: List<MonthSummary>,
    progress: Float,
    selectedMonth: Int?,
    pulse: Float,
    ghostColor: Color
): List<MonthInk> = months.mapIndexed { index, summary ->
    val dim = if (selectedMonth != null && selectedMonth != summary.month) 0.55f else 1f

    when {
        summary.dominant.isNotEmpty() -> {
            // Mỗi tháng trễ một nhịp so với tháng trước nên màu lần lượt nở ra.
            val local = ((progress * (12 + 4) - index) / 4f).coerceIn(0f, 1f)
            val grow = FastOutSlowInEasing.transform(local)
            val strength = if (summary.status == MonthStatus.IN_PROGRESS) 0.60f + pulse else 1f
            MonthInk(summary.dominant.map { it.accent }, strength * grow * dim)
        }
        // Lớp mờ nền: để cây vẫn nhận ra hình khi tháng chưa có màu.
        summary.status == MonthStatus.EMPTY -> MonthInk(listOf(ghostColor), 0.30f * dim)
        else -> MonthInk(listOf(ghostColor), 0.14f * dim)
    }
}

/** Hàng 12 chấm tròn: lối vào thay cho việc chạm đúng vùng nhỏ trên tán cây. */
@Composable
private fun MonthChips(
    months: List<MonthSummary>,
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        months.forEach { summary ->
            val isSelected = selected == summary.month
            val fill: Brush = when {
                summary.dominant.isEmpty() -> SolidColor(colors.onSurface.copy(alpha = 0.08f))
                summary.dominant.size == 1 -> SolidColor(summary.dominant.first().accent)
                else -> Brush.horizontalGradient(summary.dominant.map { it.accent })
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(summary.month) }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .semantics { contentDescription = "Tháng ${summary.month}" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .then(
                            if (isSelected) Modifier.border(2.5.dp, colors.primary, CircleShape)
                            else Modifier
                        )
                )
                Text(
                    text = "T${summary.month}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthCard(
    summary: MonthSummary
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface.copy(alpha = 0.9f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Tháng ${summary.month}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (summary.total == 0) {
            Text(
                text = if (summary.status == MonthStatus.FUTURE) {
                    "Tháng này chưa tới."
                } else {
                    "Tháng này chưa có nhật ký, vùng này để trắng."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        } else {
            summary.counts.entries.sortedByDescending { it.value }.forEach { (emotion, count) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmotionIcon(emotion = emotion, size = 28.dp)
                    Text(
                        text = emotion.label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    )
                    Text(text = "$count ngày", color = colors.onSurfaceVariant)
                }
            }

            if (summary.dominant.size > 1) {
                Text(
                    text = "Các cảm xúc ngang nhau, màu của tháng là sự pha trộn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            if (summary.status == MonthStatus.IN_PROGRESS) {
                Text(
                    text = "Tháng này đang nở, màu sẽ đậm hẳn khi tháng trôi qua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/** Chú giải dùng nhân vật của bộ biểu tượng đang chọn. */
@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Emotion.entries.forEach { emotion ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmotionIcon(emotion = emotion, size = 40.dp)
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(emotion.accent)
                )
                Text(
                    text = emotion.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}