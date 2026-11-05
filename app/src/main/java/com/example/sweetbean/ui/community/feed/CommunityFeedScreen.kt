package com.example.sweetbean.ui.community.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sweetbean.data.community.DistributionLevel
import com.example.sweetbean.data.community.Post
import com.example.sweetbean.data.community.PostVisibility
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.community.CommunityFormat
import com.example.sweetbean.ui.community.components.PostMediaGrid
import com.example.sweetbean.ui.community.components.PostMenu
import com.example.sweetbean.ui.community.components.UserAvatar
import kotlinx.coroutines.launch

@Composable
fun CommunityFeedScreen(
    uiState: CommunityFeedUiState,
    currentUid: String,
    onTabSelected: (FeedTab) -> Unit,
    onLikeClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onCreateClick: () -> Unit,
    onRefresh: () -> Unit,
    onPostClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onHideClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    val barHeight = LocalFloatingBarHeight.current
    val tabs = FeedTab.entries
    val pagerState = rememberPagerState(initialPage = uiState.tab.ordinal, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Vuốt xong thì báo ViewModel biết đang ở tab nào.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> onTabSelected(tabs[page]) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thay chữ này bằng logo SweetBean nếu bạn đã có drawable.
                Text(
                    text = "SweetBean",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                )
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            FeedTabs(
                selectedIndex = pagerState.currentPage,
                onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
            )
            Spacer(Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val tab = tabs[page]
                FeedPage(
                    tab = tab,
                    state = uiState.tabState(tab),
                    followingIds = uiState.followingIds,
                    currentUid = currentUid,
                    barHeight = barHeight,
                    onLikeClick = onLikeClick,
                    onFollowClick = onFollowClick,
                    onLoadMore = onLoadMore,
                    onRefresh = onRefresh,
                    onPostClick = onPostClick,
                    onDeleteClick = onDeleteClick,
                    onHideClick = onHideClick,
                    onEditClick = onEditClick
                )
            }
        }

        FloatingActionButton(
            onClick = onCreateClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = barHeight + 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tạo bài viết")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPage(
    tab: FeedTab,
    state: FeedTabState,
    followingIds: Set<String>,
    currentUid: String,
    barHeight: Dp,
    onLikeClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onPostClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onHideClick: (String) -> Unit,
    onEditClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.items.size) {
        if (tab == FeedTab.ALL && nearEnd) onLoadMore()
    }

    // Vòng kéo chỉ hiện khi tải lại trên danh sách đã có nội dung.
    val isRefreshing = state.isLoading && state.items.isNotEmpty()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.items.isEmpty() && (state.isLoading || (!state.loaded && !state.failed)) ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            state.items.isEmpty() -> EmptyFeed(tab, state, followingIds)

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = barHeight + 88.dp)
            ) {
                items(state.items, key = { it.post.id }) { item ->
                    PostCard(
                        item = item,
                        isOwn = item.post.authorId == currentUid,
                        isFollowing = item.post.authorId in followingIds,
                        onClick = { onPostClick(item.post.id) },
                        onLike = { onLikeClick(item.post.id) },
                        onFollow = { onFollowClick(item.post.authorId) },
                        onEdit = { onEditClick(item.post.id) },
                        onDelete = { onDeleteClick(item.post.id) },
                        onHide = { onHideClick(item.post.id) }
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTabs(selectedIndex: Int, onSelected: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceVariant)
            .padding(4.dp)
    ) {
        FeedTab.entries.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyFeed(tab: FeedTab, state: FeedTabState, followingIds: Set<String>) {
    val message = when {
        state.failed -> "Không tải được bảng tin.\nKéo xuống để thử lại."
        tab == FeedTab.FOLLOWING && followingIds.isEmpty() ->
            "Bạn chưa theo dõi ai.\nSang tab Tất cả để khám phá nhé."
        tab == FeedTab.FOLLOWING ->
            "Những người bạn theo dõi chưa có bài viết mới.\nKéo xuống để tải lại."
        else -> "Chưa có bài viết nào.\nHãy là người đầu tiên chia sẻ!"
    }
    // Phải cuộn được thì mới kéo làm mới được khi danh sách trống.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PostCard(
    item: FeedItem,
    isOwn: Boolean,
    isFollowing: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onFollow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val name = item.author?.displayName?.ifBlank { null } ?: "Người dùng SweetBean"
    val username = item.author?.username.orEmpty()
    val note = if (isOwn) ownerNote(item.post) else null

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(url = item.author?.avatarUrl, name = name, size = 44.dp)
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
                            append(CommunityFormat.timeAgo(item.post.createdAtMillis))
                            if (item.post.editedAtMillis != null) append(" · đã chỉnh sửa")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurfaceVariant
                    )
                }
                if (!isOwn) {
                    FollowChip(following = isFollowing, onClick = onFollow)
                }
                PostMenu(isOwn = isOwn, onEdit = onEdit, onDelete = onDelete, onHide = onHide)
            }

            if (note != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (item.post.text.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = item.post.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }

            if (item.post.media.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                PostMediaGrid(item.post.media)
            }

            Spacer(Modifier.height(10.dp))
            PostActions(item = item, onLike = onLike)
        }
    }
}

private fun ownerNote(post: Post): String? = when {
    post.distributionLevel == DistributionLevel.AUTHOR_ONLY && post.moderationLabel == null ->
        "⏳ Đang chờ kiểm duyệt"
    post.distributionLevel == DistributionLevel.AUTHOR_ONLY -> "🔒 Chỉ mình bạn thấy"
    post.distributionLevel == DistributionLevel.LIMITED -> "👥 Chỉ người theo dõi bạn thấy"
    post.visibility == PostVisibility.FOLLOWERS -> "👥 Chỉ người theo dõi"
    else -> null
}

@Composable
internal fun FollowChip(following: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (following) colors.primaryContainer else colors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (following) "Đang theo dõi" else "Theo dõi",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (following) colors.onPrimaryContainer else colors.onPrimary
        )
    }
}

@Composable
private fun PostActions(item: FeedItem, onLike: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onLike)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Thích",
                tint = if (item.isLiked) colors.tertiary else colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = item.post.likeCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
        }

        Text(
            text = "💬 ${item.post.commentCount}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant
        )
        Text(
            text = "🔁 ${item.post.repostCount}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant
        )
    }
}