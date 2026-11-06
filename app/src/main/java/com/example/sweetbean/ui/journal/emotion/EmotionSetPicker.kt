package com.example.sweetbean.ui.journal.emotion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sweetbean.data.journal.Emotion
import com.example.sweetbean.ui.journal.components.EmotionIcon
import com.example.sweetbean.ui.journal.components.EmotionIconOf

/** Nút tròn nhỏ hiện biểu tượng "Vui" của bộ đang dùng; bấm để mở bộ chọn. */
@Composable
fun EmotionSetButton(modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable { show = true }
            .semantics { contentDescription = "Đổi bộ biểu tượng cảm xúc" },
        contentAlignment = Alignment.Center
    ) {
        EmotionIcon(emotion = Emotion.HAPPY, size = 28.dp)
    }

    if (show) EmotionSetPickerDialog(onDismiss = { show = false })
}

@Composable
fun EmotionSetPickerDialog(onDismiss: () -> Unit) {
    val current = LocalEmotionSet.current
    val change = LocalEmotionSetChanger.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bộ biểu tượng cảm xúc",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Đổi bộ không làm mất dữ liệu: các nhật ký cũ vẫn giữ nguyên cảm xúc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EmotionSets.all.forEach { set ->
                    SetRow(
                        set = set,
                        selected = set.id == current.id,
                        onClick = { change(set) }
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Xong")
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    set: EmotionSet,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) colors.primary.copy(alpha = 0.12f)
                else colors.surfaceVariant.copy(alpha = 0.5f)
            )
            .then(
                if (selected) Modifier.border(2.dp, colors.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = set.label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Text(
                    text = "Đang dùng",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary
                )
            }
        }

        // Xem trước đủ 5 cảm xúc của chính bộ này, không phải bộ đang dùng.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Emotion.entries.forEach { emotion ->
                EmotionIconOf(set = set, emotion = emotion, size = 48.dp)
            }
        }
    }
}