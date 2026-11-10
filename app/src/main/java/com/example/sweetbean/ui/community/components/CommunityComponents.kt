package com.example.sweetbean.ui.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sweetbean.data.journal.JournalMedia
import com.example.sweetbean.ui.journal.components.MediaThumb
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.sweetbean.data.community.ReportLimits
import com.example.sweetbean.data.community.ReportReason
import com.example.sweetbean.data.community.ReportTarget
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.OutlinedButton

@Composable
fun UserAvatar(
    url: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val base = modifier
        .size(size)
        .clip(CircleShape)

    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = base
        )
    } else {
        Box(
            modifier = base.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Màn nào cũng có thể gọi (loại đối tượng, id) để mở hộp thoại báo cáo; Route cung cấp hàm thật. */
val LocalReportHandler = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

@Composable
fun PostMenu(
    isOwn: Boolean,
    onDelete: () -> Unit,
    onReport: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Tùy chọn",
                tint = colors.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (isOwn) {
                DropdownMenuItem(
                    text = { Text("Xóa bài viết", color = colors.error) },
                    onClick = {
                        open = false
                        confirmDelete = true
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Báo cáo bài viết", color = colors.error) },
                    onClick = {
                        open = false
                        onReport()
                    }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa bài viết?") },
            text = { Text("Bài viết sẽ biến mất khỏi SweetBean.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Xóa", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Hủy") }
            }
        )
    }
}

/**
 * Lưới media dùng chung: 1 ảnh = khung lớn, 2 = hai cột, 3 = một lớn + hai nhỏ,
 * từ 4 trở lên = lưới 2x2 (ô cuối hiện "+N"). Truyền [onMediaClick] để chạm vào từng ô.
 */
@Composable
fun PostMediaGrid(
    media: List<JournalMedia>,
    modifier: Modifier = Modifier,
    onMediaClick: ((Int) -> Unit)? = null
) {
    if (media.isEmpty()) return
    val gap = 4.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        when (media.size) {
            1 -> MediaTile(
                media, 0, Modifier.fillMaxWidth().aspectRatio(4f / 3f), 0, onMediaClick
            )

            2 -> Row(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                MediaTile(media, 0, Modifier.weight(1f).fillMaxHeight(), 0, onMediaClick)
                MediaTile(media, 1, Modifier.weight(1f).fillMaxHeight(), 0, onMediaClick)
            }

            3 -> Row(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                MediaTile(media, 0, Modifier.weight(2f).fillMaxHeight(), 0, onMediaClick)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    MediaTile(media, 1, Modifier.weight(1f).fillMaxWidth(), 0, onMediaClick)
                    MediaTile(media, 2, Modifier.weight(1f).fillMaxWidth(), 0, onMediaClick)
                }
            }

            else -> Column(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    MediaTile(media, 0, Modifier.weight(1f).fillMaxHeight(), 0, onMediaClick)
                    MediaTile(media, 1, Modifier.weight(1f).fillMaxHeight(), 0, onMediaClick)
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    MediaTile(media, 2, Modifier.weight(1f).fillMaxHeight(), 0, onMediaClick)
                    MediaTile(
                        media, 3, Modifier.weight(1f).fillMaxHeight(), media.size - 4, onMediaClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaTile(
    media: List<JournalMedia>,
    index: Int,
    modifier: Modifier,
    extra: Int,
    onClick: ((Int) -> Unit)?
) {
    Box(modifier = if (onClick != null) modifier.clickable { onClick(index) } else modifier) {
        MediaThumb(
            model = media[index].thumbnailUrl,
            isVideo = media[index].isVideo,
            modifier = Modifier.fillMaxSize()
        )
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extra",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun ReportDialog(
    targetType: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason, String, List<Uri>) -> Unit
) {
    var selected by remember { mutableStateOf<ReportReason?>(null) }
    var description by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ReportLimits.MAX_EVIDENCE)
    ) { uris ->
        evidence = (evidence + uris).distinct().take(ReportLimits.MAX_EVIDENCE)
    }

    val title = when (targetType) {
        ReportTarget.POST -> "Báo cáo bài viết"
        ReportTarget.COMMENT -> "Báo cáo bình luận"
        else -> "Báo cáo tài khoản"
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Chọn một lý do:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSubmitting) { selected = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason },
                            enabled = !isSubmitting
                        )
                        Text(reason.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(ReportLimits.MAX_DESCRIPTION_LENGTH) },
                    label = { Text("Mô tả thêm (không bắt buộc)") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Ảnh bằng chứng (không bắt buộc, tối đa ${ReportLimits.MAX_EVIDENCE})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    evidence.forEach { uri ->
                        Box {
                            MediaThumb(
                                model = uri,
                                isVideo = false,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(enabled = !isSubmitting) { evidence = evidence - uri },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Bỏ ảnh",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    if (evidence.size < ReportLimits.MAX_EVIDENCE) {
                        OutlinedButton(
                            onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isSubmitting
                        ) { Text("🖼  Thêm ảnh") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onSubmit(it, description, evidence) } },
                enabled = selected != null && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Gửi báo cáo")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Hủy") }
        }
    )
}