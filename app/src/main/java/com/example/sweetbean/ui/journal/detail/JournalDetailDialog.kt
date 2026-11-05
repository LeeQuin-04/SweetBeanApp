package com.example.sweetbean.ui.journal.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sweetbean.data.journal.Journal
import com.example.sweetbean.ui.journal.JournalFormat
import com.example.sweetbean.ui.journal.components.EmotionBadge
import com.example.sweetbean.ui.journal.components.MediaThumb
import com.example.sweetbean.ui.journal.components.accent

@Composable
fun JournalDetailDialog(
    journal: Journal,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    val accent = journal.emotion.accent
    val error = MaterialTheme.colorScheme.error

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 680.dp)
        ) {
            Box {
                // Quầng sáng màu cảm xúc phía sau phần đầu.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EmotionBadge(emotion = journal.emotion, size = 96.dp)

                        Text(
                            text = journal.title.ifBlank { journal.emotion.label },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = JournalFormat.full(journal.dateMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (journal.text.isNotBlank()) {
                            Text(
                                text = journal.text,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 25.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )
                        }

                        if (journal.media.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                journal.media.forEachIndexed { index, media ->
                                    MediaThumb(
                                        model = media.thumbnailUrl,
                                        isVideo = media.isVideo,
                                        modifier = Modifier
                                            .size(width = 116.dp, height = 140.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { viewerIndex = index }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onEdit,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(alpha = 0.22f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, Modifier.size(18.dp))
                            Text("  Chỉnh sửa", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, error.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = error),
                            modifier = Modifier
                                .weight(0.75f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, Modifier.size(18.dp))
                            Text("  Xóa", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa nhật ký?") },
            text = { Text("Nhật ký sẽ bị xóa vĩnh viễn và không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Xóa", color = error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Hủy") }
            }
        )
    }

    viewerIndex?.let { index ->
        MediaViewerDialog(
            media = journal.media,
            startIndex = index,
            onDismiss = { viewerIndex = null }
        )
    }
}