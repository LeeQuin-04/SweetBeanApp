package com.example.sweetbean.ui.community.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.community.PostLimits
import com.example.sweetbean.data.community.PostVisibility
import com.example.sweetbean.ui.community.components.UserAvatar
import com.example.sweetbean.ui.journal.components.MediaThumb
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import com.example.sweetbean.ui.common.LocalFloatingBarHeight

@Composable
fun CreatePostScreen(
    uiState: CreatePostUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onAddMedia: (List<Uri>, Boolean) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onPost: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val enabled = !uiState.isPosting
    val barHeight = LocalFloatingBarHeight.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current)>0

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PostLimits.MAX_MEDIA)
    ) { uris -> if (uris.isNotEmpty()) onAddMedia(uris, false) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PostLimits.MAX_MEDIA)
    ) { uris -> if (uris.isNotEmpty()) onAddMedia(uris, true) }

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
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Tạo bài viết",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }

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

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) { Text("🖼  Ảnh") }

            OutlinedButton(
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) { Text("🎬  Video") }
        }

        Spacer(Modifier.height(12.dp))

        if (uiState.media.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có ảnh hoặc video", color = colors.onSurfaceVariant)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.media, key = { it.key }) { picked ->
                    Box {
                        MediaThumb(
                            model = picked.uri,
                            isVideo = picked.isVideo,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable(enabled = enabled) { onRemoveMedia(picked.key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Bỏ ảnh/video",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        VisibilityPicker(
            visibility = uiState.visibility,
            enabled = enabled,
            onChange = onVisibilityChange
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onPost,
            enabled = uiState.canPost,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (uiState.isPosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = colors.onPrimary
                )
            } else {
                Text("ĐĂNG BÀI", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun VisibilityPicker(
    visibility: String,
    enabled: Boolean,
    onChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }

    val options = listOf(
        PostVisibility.PUBLIC to "🌐  Công khai",
        PostVisibility.FOLLOWERS to "🔒  Người theo dõi"
    )
    val label = options.firstOrNull { it.first == visibility }?.second ?: options.first().second

    Box {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { open = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.weight(1f), color = colors.onSurface)
                Text("›", color = colors.onSurfaceVariant)
            }
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onChange(value)
                        open = false
                    }
                )
            }
        }
    }
}