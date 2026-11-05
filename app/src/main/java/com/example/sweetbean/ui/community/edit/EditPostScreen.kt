package com.example.sweetbean.ui.community.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.community.PostLimits
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.community.components.PostMediaGrid
import com.example.sweetbean.ui.community.components.UserAvatar
import com.example.sweetbean.ui.community.create.VisibilityPicker

@Composable
fun EditPostScreen(
    uiState: EditPostUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val barHeight = LocalFloatingBarHeight.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val post = uiState.post
    val enabled = !uiState.isSaving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = if (imeVisible) 24.dp else barHeight + 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = enabled) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Chỉnh sửa bài viết",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }

        when (post) {
            null if uiState.isLoading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            null -> Text(
                text = "Không tìm thấy bài viết hoặc bạn không có quyền chỉnh sửa.",
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp)
            )
            else -> {
                val name = uiState.author?.displayName?.ifBlank { null } ?: "Bạn"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(url = uiState.author?.avatarUrl, name = name, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        val username = uiState.author?.username.orEmpty()
                        if (username.isNotBlank()) {
                            Text(
                                "@$username",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = onTextChange,
                    enabled = enabled,
                    placeholder = { Text("Bạn đang nghĩ gì?") },
                    minLines = 6,
                    maxLines = 12,
                    shape = RoundedCornerShape(18.dp),
                    supportingText = {
                        Text(
                            text = "${uiState.text.length}/${PostLimits.MAX_TEXT_LENGTH}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.text.trim() != post.text.trim()) {
                    Text(
                        text = "Nội dung sẽ được kiểm duyệt lại khi bạn lưu thay đổi.",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                if (post.media.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    PostMediaGrid(post.media)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Ảnh/video không thể thay đổi khi chỉnh sửa.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                VisibilityPicker(
                    visibility = uiState.visibility,
                    enabled = enabled,
                    onChange = onVisibilityChange
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimary
                        )
                    } else {
                        Text("LƯU THAY ĐỔI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Preview(showBackground=true, showSystemUi = true)
@Composable
fun EditPosScreenPreview(){
    EditPostScreen(
        uiState = EditPostUiState(),
        onBack = {},
        onTextChange = {},
        onVisibilityChange ={},
        onSave={}
    )
}