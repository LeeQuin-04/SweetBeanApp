package com.example.sweetbean.ui.community.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.community.Post
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.community.CommunityFormat
import com.example.sweetbean.ui.community.components.PostMediaGrid
import com.example.sweetbean.ui.community.components.PostMenu
import com.example.sweetbean.ui.community.components.UserAvatar
import com.example.sweetbean.ui.community.feed.FollowChip
import com.example.sweetbean.ui.journal.detail.MediaViewerDialog
import com.example.sweetbean.data.community.ReportTarget
import com.example.sweetbean.ui.community.components.LocalReportHandler

@Composable
fun PostDetailScreen(
    uiState: PostDetailUiState,
    currentUid: String,
    onBack: () -> Unit,
    onPostLike: () -> Unit,
    onFollow: () -> Unit,
    onDeletePost: () -> Unit,
    onEditPost: ()-> Unit,
    onHidePost: () -> Unit,
    onCommentLike: (String) -> Unit,
    onReply: (CommentItem, CommentItem) -> Unit,
    onToggleThread: (String) -> Unit,
    onCancelReply: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val barHeight = LocalFloatingBarHeight.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val post = uiState.post
    val report = LocalReportHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Bài viết",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (post != null) {
                PostMenu(
                    isOwn = post.authorId == currentUid,
                    onDelete = onDeletePost,
                    onHide = onHidePost,
                    onEdit = onEditPost,
                    onReport = { report(ReportTarget.POST, post.id) }
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (post) {
                null if uiState.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                null -> Text(
                    text = if (uiState.notFound) {
                        "Bài viết không còn tồn tại hoặc bạn không có quyền xem."
                    } else {
                        "Không tải được bài viết."
                    },
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        PostBody(
                            uiState = uiState,
                            post = post,
                            currentUid = currentUid,
                            onPostLike = onPostLike,
                            onFollow = onFollow,
                            onAuthorClick = { onAuthorClick(post.authorId) }
                        )
                    }
                    item {
                        Text(
                            text = "Bình luận (${post.commentCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }
                    if (uiState.threads.isEmpty()) {
                        item {
                            Text(
                                text = "Chưa có bình luận nào. Hãy là người đầu tiên!",
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    items(uiState.threads, key = { it.root.comment.id }) { thread ->
                        ThreadView(
                            thread = thread,
                            expanded = thread.root.comment.id in uiState.expandedThreads,
                            currentUid = currentUid,
                            postAuthorId = post.authorId,
                            replyTargetId = uiState.replyTarget?.comment?.id,
                            onCommentLike = onCommentLike,
                            onReply = onReply,
                            onToggle = { onToggleThread(thread.root.comment.id) },
                            onAuthorClick = onAuthorClick
                        )
                    }
                }
            }
        }

        if (post != null) {
            CommentInput(
                uiState = uiState,
                bottomPadding = if (imeVisible) 8.dp else barHeight,
                onCancelReply = onCancelReply,
                onInputChange = onInputChange,
                onSend = onSend
            )
        }
    }
}

@Composable
private fun PostBody(
    uiState: PostDetailUiState,
    post: Post,
    currentUid: String,
    onPostLike: () -> Unit,
    onFollow: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val name = uiState.author?.displayName?.ifBlank { null } ?: "Người dùng SweetBean"
    val username = uiState.author?.username.orEmpty()
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAuthorClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(url = uiState.author?.avatarUrl, name = name, size = 46.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                    Text(
                        text = buildString {
                            if (username.isNotBlank()) append("@$username · ")
                            append(CommunityFormat.timeAgo(post.createdAtMillis))
                            if (post.editedAtMillis != null) append(" · đã chỉnh sửa")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            if (post.authorId != currentUid) {
                FollowChip(following = uiState.isFollowingAuthor, onClick = onFollow)
            }
        }

        if (post.text.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )
        }

        if (post.media.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PostMediaGrid(
                media = post.media,
                onMediaClick = { viewerIndex = it }
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onPostLike)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.isPostLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Thích",
                    tint = if (uiState.isPostLiked) colors.tertiary else colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = post.likeCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                text = "💬 ${post.commentCount}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
            Text(
                text = "🔁 ${post.repostCount}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
        }
    }

    viewerIndex?.let { index ->
        MediaViewerDialog(
            media = post.media,
            startIndex = index,
            onDismiss = { viewerIndex = null }
        )
    }
}

@Composable
private fun ThreadView(
    thread: CommentThread,
    expanded: Boolean,
    currentUid: String,
    postAuthorId: String,
    replyTargetId: String?,
    onCommentLike: (String) -> Unit,
    onReply: (CommentItem, CommentItem) -> Unit,
    onAuthorClick: (String) -> Unit,
    onToggle: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val report = LocalReportHandler.current

    Column {
        CommentRow(
            item = thread.root,
            isReply = false,
            currentUid = currentUid,
            postAuthorId = postAuthorId,
            highlighted = replyTargetId == thread.root.comment.id,
            onLike = { onCommentLike(thread.root.comment.id) },
            onReply = { onReply(thread.root, thread.root) },
            onAuthorClick = onAuthorClick
        )

        // Thu gọn: chỉ hiện reply đầu tiên, phần còn lại mở bằng "Xem thêm".
        val shown = if (expanded) thread.replies else thread.replies.take(1)
        if (shown.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 42.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shown.forEach { reply ->
                    CommentRow(
                        item = reply,
                        isReply = true,
                        currentUid = currentUid,
                        postAuthorId = postAuthorId,
                        highlighted = replyTargetId == reply.comment.id,
                        onLike = { onCommentLike(reply.comment.id) },
                        onReply = { onReply(thread.root, reply) },
                        onAuthorClick = onAuthorClick
                    )
                }
                if (thread.replies.size > 1) {
                    Text(
                        text = if (expanded) "Thu gọn" else "Xem thêm ${thread.replies.size - 1} câu trả lời",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                        modifier = Modifier
                            .clickable(onClick = onToggle)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    item: CommentItem,
    isReply: Boolean,
    currentUid: String,
    postAuthorId: String,
    highlighted: Boolean,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val report = LocalReportHandler.current
    val colors = MaterialTheme.colorScheme
    val comment = item.comment
    val name = item.author?.displayName?.ifBlank { null } ?: "Người dùng SweetBean"
    val username = item.author?.username.orEmpty()
    // Chỉ hiện @username khi khác tên hiển thị.
    val showUsername = username.isNotBlank() && !username.equals(name, ignoreCase = true)
    val isPostAuthor = comment.authorId == postAuthorId

    // Người đăng thấy bình luận của mình ở mọi nhãn, kèm ghi chú trung tính.
    val note = when {
        comment.authorId != currentUid -> null
        comment.moderationLabel == "CLEAN" -> null
        comment.moderationLabel == "OFFENSIVE" -> "👥 Chỉ người theo dõi bạn thấy"
        comment.moderationLabel == null -> "⏳ Đang chờ kiểm duyệt"
        else -> "🔒 Chỉ mình bạn thấy"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) colors.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
            .padding(4.dp)
    ) {
        UserAvatar(
            url = item.author?.avatarUrl,
            name = name,
            size = if (isReply) 30.dp else 38.dp,
            modifier = Modifier.clickable { onAuthorClick(comment.authorId) }
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onAuthorClick(comment.authorId) }
                )
                if (isPostAuthor) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.primary)
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Tác giả",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
            Text(
                text = buildString {
                    if (showUsername) append("@$username · ")
                    append(CommunityFormat.timeAgo(comment.createdAtMillis))
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = mentionText(comment.text, colors.primary),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onLike)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Thích bình luận",
                        tint = if (item.isLiked) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = comment.likeCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isLiked) colors.primary else colors.onSurfaceVariant
                    )
                }
                Text(
                    text = "Trả lời",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onReply)
                        .padding(vertical = 2.dp)
                )
                if (comment.authorId != currentUid) {
                    Text(
                        text = "Báo cáo",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { report(ReportTarget.COMMENT, comment.id) }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private val MentionRegex = Regex("^@[A-Za-z0-9_]{3,20}")

/** Tô màu @username ở đầu bình luận để biết đang trả lời ai. */
private fun mentionText(text: String, color: Color): AnnotatedString = buildAnnotatedString {
    val match = MentionRegex.find(text)
    if (match == null) {
        append(text)
    } else {
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(match.value) }
        append(text.removePrefix(match.value))
    }
}

@Composable
private fun CommentInput(
    uiState: PostDetailUiState,
    bottomPadding: Dp,
    onCancelReply: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = bottomPadding)
    ) {
        val target = uiState.replyTarget ?: uiState.replyingTo
        if (target != null) {
            val targetName = target.author?.displayName?.ifBlank { null }
                ?: target.author?.username
                ?: "người dùng"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đang trả lời $targetName",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hủy trả lời",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onCancelReply)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.input,
                onValueChange = onInputChange,
                enabled = !uiState.isSending,
                placeholder = { Text("Viết bình luận...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSend,
                enabled = uiState.input.isNotBlank() && !uiState.isSending
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi",
                        tint = if (uiState.input.isNotBlank()) colors.primary else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}