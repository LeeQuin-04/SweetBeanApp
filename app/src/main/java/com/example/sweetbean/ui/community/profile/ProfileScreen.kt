package com.example.sweetbean.ui.community.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.community.PublicProfile
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.community.components.UserAvatar
import com.example.sweetbean.ui.community.feed.PostCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.sweetbean.data.community.ReportTarget
import com.example.sweetbean.ui.community.components.LocalReportHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    currentUid: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onFollowClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val barHeight = LocalFloatingBarHeight.current
    val listState = rememberLazyListState()
    val profile = uiState.profile
    val report = LocalReportHandler.current

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, uiState.items.size) {
        if (nearEnd) onLoadMore()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Trang cá nhân",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (profile != null && !uiState.isOwn) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Báo cáo tài khoản", color = colors.error) },
                            onClick = {
                                menuOpen = false
                                report(ReportTarget.USER, profile.id)
                            }
                        )
                    }
                }
            }
        }

        when (profile) {
            null if uiState.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.notFound) {
                        "Không tìm thấy người dùng này."
                    } else {
                        "Không tải được trang cá nhân."
                    },
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = uiState.isLoadingPosts && uiState.items.isNotEmpty(),
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = barHeight + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ProfileHeader(
                            profile = profile,
                            isOwn = uiState.isOwn,
                            isFollowing = uiState.isFollowing,
                            onFollowClick = onFollowClick
                        )
                    }
                    item {
                        Column {
                            Text(
                                text = "Bài viết",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            if (!uiState.isOwn && !uiState.isFollowing) {
                                Text(
                                    text = "Theo dõi để xem cả các bài dành riêng cho người theo dõi.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (uiState.items.isEmpty()) {
                        item {
                            if (uiState.isLoadingPosts) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp) }
                            } else {
                                Text(
                                    text = "Chưa có bài viết nào.",
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                    items(uiState.items, key = { it.post.id }) { item ->
                        PostCard(
                            item = item,
                            isOwn = item.post.authorId == currentUid,
                            isFollowing = uiState.isFollowing,
                            onClick = { onPostClick(item.post.id) },
                            onLike = { onLikeClick(item.post.id) },
                            onFollow = {},
                            onDelete = { onDeleteClick(item.post.id) },
                            showFollow = false
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: PublicProfile,
    isOwn: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val name = profile.displayName.ifBlank { profile.username.ifBlank { "Người dùng SweetBean" } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(url = profile.avatarUrl, name = name, size = 96.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        if (profile.username.isNotBlank()) {
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "“${profile.bio}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Stat(value = profile.followingCount, label = "Đang theo dõi")
            Stat(value = profile.followersCount, label = "Người theo dõi")
            Stat(value = profile.postsCount, label = "Bài viết")
        }

        if (!isOwn) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = if (isFollowing) {
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = if (isFollowing) "Đang theo dõi" else "Theo dõi",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Stat(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}