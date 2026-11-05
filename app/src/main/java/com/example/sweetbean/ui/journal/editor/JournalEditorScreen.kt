package com.example.sweetbean.ui.journal.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.journal.Emotion
import com.example.sweetbean.data.journal.JournalLimits
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.journal.components.EmotionPicker
import com.example.sweetbean.ui.journal.components.JournalBackdrop
import com.example.sweetbean.ui.journal.components.MediaThumb
import com.example.sweetbean.ui.journal.components.MoodHero
import com.example.sweetbean.ui.journal.components.accentOrBrand
import com.example.sweetbean.ui.journal.components.prompt
import com.example.sweetbean.ui.journal.components.JournalScene
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalDensity

@Composable
fun JournalEditorScreen(
    uiState: JournalEditorUiState,
    onBack: () -> Unit,
    onEmotionSelected: (Emotion) -> Unit,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onAddMedia: (List<Uri>) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onSave: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(JournalLimits.MAX_MEDIA)
    ) { uris ->
        if (uris.isNotEmpty()) onAddMedia(uris)
    }

    // Cả form đổi màu theo cảm xúc đang chọn.
    val accent by animateColorAsState(
        targetValue = uiState.emotion.accentOrBrand(),
        animationSpec = tween(450),
        label = "moodAccent"
    )

    val isEditing = uiState.editingId != null
    val fieldShape = RoundedCornerShape(20.dp)
    val fieldColors = moodFieldColors(accent)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val barPadding by animateDpAsState(
        targetValue = if (imeVisible) 0.dp else LocalFloatingBarHeight.current,
        label = "barPadding"
    )

    if (uiState.isLoading) {
        JournalBackdrop(scene = JournalScene.Editor) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        }
        return
    }

    JournalBackdrop(scene = JournalScene.Editor) {
        // Quầng sáng màu cảm xúc phía trên.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(bottom = barPadding)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                }
                Text(
                    text = if (isEditing) "Chỉnh sửa nhật ký" else "Tạo nhật ký",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = !imeVisible) {
                    Column {
                        MoodHero(emotion = uiState.emotion)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                EmotionPicker(
                    selected = uiState.emotion,
                    onSelected = onEmotionSelected,
                    enabled = !uiState.isSaving
                )

                Spacer(Modifier.height(16.dp))

                FieldLabel("Tiêu đề", hint = "(tùy chọn)")
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    placeholder = { Text("Đặt tên cho ngày hôm nay") },
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                FieldLabel("Nội dung")
                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = onTextChange,
                    placeholder = { Text(uiState.emotion?.prompt ?: "Bạn muốn viết gì hôm nay?") },
                    minLines = 5,
                    enabled = !uiState.isSaving,
                    shape = fieldShape,
                    colors = fieldColors,
                    supportingText = {
                        Text(
                            text = "${uiState.text.length}/${JournalLimits.MAX_TEXT_LENGTH}",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                FieldLabel("Ảnh và video")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.media.size < JournalLimits.MAX_MEDIA) {
                        AddMediaTile(
                            accent = accent,
                            enabled = !uiState.isSaving,
                            onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                    )
                                )
                            }
                        )
                    }

                    uiState.media.forEach { item ->
                        key(item.key) {
                            Box(modifier = Modifier.size(88.dp)) {
                                MediaThumb(
                                    model = item.previewModel(),
                                    isVideo = item.isVideo,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(5.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .clickable(enabled = !uiState.isSaving) {
                                            onRemoveMedia(item.key)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Bỏ ảnh/video",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            val onAccent = if (accent.luminance() > 0.4f) Color(0xFF2A2350) else Color.White
            val ink = MaterialTheme.colorScheme.onSurface

            Button(
                onClick = onSave,
                enabled = uiState.emotion != null && !uiState.isSaving,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = onAccent,
                    disabledContainerColor = ink.copy(alpha = 0.12f),
                    disabledContentColor = ink.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(56.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = onAccent
                    )
                } else {
                    Text(
                        text = if (isEditing) "Lưu thay đổi" else "Lưu nhật ký",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun moodFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
    cursorColor = accent
)

@Composable
private fun FieldLabel(title: String, hint: String? = null) {
    Row(modifier = Modifier.padding(top = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Text(
                text = " $hint",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddMediaTile(accent: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.08f))
            .drawBehind {
                drawRoundRect(
                    color = accent.copy(alpha = 0.6f),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                )
            }
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = accent)
        Text(
            text = "Thêm",
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
    }
}

/** Video đang ở máy chưa có ảnh bìa nên chỉ hiện biểu tượng phát. */
private fun EditorMedia.previewModel(): Any? = when (this) {
    is EditorMedia.Remote -> media.thumbnailUrl
    is EditorMedia.Local -> if (isVideo) null else uri
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JournalEditorScreenPreview() {
    JournalEditorScreen(
        uiState = JournalEditorUiState(),
        onBack = {},
        onEmotionSelected = {},
        onTitleChange = {},
        onTextChange = {},
        onAddMedia = {},
        onRemoveMedia = {},
        onSave = {}
    )
}