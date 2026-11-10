package com.example.sweetbean.ui.community

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sweetbean.data.community.DistributionLevel
import com.example.sweetbean.data.community.PostVisibility
import com.example.sweetbean.data.community.ReviewStatus
import com.example.sweetbean.di.AuthDependencies
import com.example.sweetbean.di.CommunityDependencies
import com.example.sweetbean.ui.community.components.LocalReportHandler
import com.example.sweetbean.ui.community.components.ReportDialog
import com.example.sweetbean.ui.community.create.CreatePostScreen
import com.example.sweetbean.ui.community.create.CreatePostViewModel
import com.example.sweetbean.ui.community.detail.PostDetailScreen
import com.example.sweetbean.ui.community.detail.PostDetailViewModel
import com.example.sweetbean.ui.community.feed.CommunityFeedScreen
import com.example.sweetbean.ui.community.feed.CommunityFeedViewModel
import com.example.sweetbean.ui.community.profile.ProfileScreen
import com.example.sweetbean.ui.community.profile.ProfileViewModel
import com.example.sweetbean.ui.community.report.ReportViewModel
import com.example.sweetbean.ui.community.search.SearchScreen
import com.example.sweetbean.ui.community.search.SearchViewModel

@Composable
fun CommunityRoute() {
    val context = LocalContext.current
    val uid = remember { AuthDependencies.firebaseAuth.currentUser?.uid } ?: return

    val feedViewModel: CommunityFeedViewModel = viewModel(
        key = "community_feed_$uid",
        factory = remember(uid) { CommunityDependencies.feedViewModelFactory(uid) }
    )
    val createViewModel: CreatePostViewModel = viewModel(
        key = "community_create_$uid",
        factory = remember(uid) { CommunityDependencies.createPostViewModelFactory(uid) }
    )
    val reportViewModel: ReportViewModel = viewModel(
        key = "community_report_$uid",
        factory = remember(uid) { CommunityDependencies.reportViewModelFactory(uid) }
    )

    val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val createState by createViewModel.uiState.collectAsStateWithLifecycle()
    val reportState by reportViewModel.uiState.collectAsStateWithLifecycle()

    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var reportType by rememberSaveable { mutableStateOf<String?>(null) }
    var reportTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    // Ngăn xếp điều hướng cho chi tiết bài và trang cá nhân: "post:{id}" hoặc "profile:{id}".
    var navStack by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    val push: (String) -> Unit = { entry -> navStack = ArrayList(navStack).apply { add(entry) } }
    val pop: () -> Unit = {
        navStack = ArrayList(navStack).apply { if (isNotEmpty()) removeAt(size - 1) }
    }
    val top = navStack.lastOrNull()
    val postId = if (top != null && top.startsWith("post:")) top.removePrefix("post:") else null
    val profileId = if (top != null && top.startsWith("profile:")) top.removePrefix("profile:") else null

    // Đăng xong: báo kết quả, tải lại bảng tin; bài không hiện ở Khám phá thì mở trang cá nhân của mình.
    LaunchedEffect(createState.postedLevel) {
        val level = createState.postedLevel ?: return@LaunchedEffect
        val showsInFeed = level == DistributionLevel.NORMAL &&
                createState.visibility == PostVisibility.PUBLIC
        val message = when {
            showsInFeed -> "Đã đăng bài viết."
            level == DistributionLevel.NORMAL -> "Đã đăng bài. Bài chỉ hiện với người theo dõi bạn."
            createState.moderationReviewStatus == ReviewStatus.PENDING_AI_UNAVAILABLE ->
                "Đã đăng bài, đang chờ kiểm duyệt."
            else -> "Đã đăng bài, nhưng bài viết đang bị giới hạn hiển thị."
        }
        showCreate = false
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        createViewModel.reset()
        feedViewModel.refresh()
        if (!showsInFeed) push("profile:$uid")
    }

    LaunchedEffect(feedState.errorMessage) {
        feedState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            feedViewModel.consumeError()
        }
    }

    LaunchedEffect(createState.errorMessage) {
        createState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            createViewModel.consumeError()
        }
    }

    LaunchedEffect(reportState.submitted) {
        if (reportState.submitted) {
            Toast.makeText(
                context,
                "Đã gửi báo cáo. Cảm ơn bạn đã giúp cộng đồng an toàn hơn!",
                Toast.LENGTH_LONG
            ).show()
            reportType = null
            reportTargetId = null
            reportViewModel.reset()
        }
    }

    LaunchedEffect(reportState.errorMessage) {
        reportState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            reportViewModel.consumeError()
        }
    }

    CompositionLocalProvider(
        LocalReportHandler provides { type: String, id: String ->
            reportType = type
            reportTargetId = id
        }
    ) {
        when {
            showCreate -> {
                BackHandler(enabled = !createState.isPosting) { showCreate = false }

                CreatePostScreen(
                    uiState = createState,
                    onBack = { showCreate = false },
                    onTextChange = createViewModel::onTextChange,
                    onVisibilityChange = createViewModel::onVisibilityChange,
                    onAddMedia = createViewModel::addMedia,
                    onRemoveMedia = createViewModel::removeMedia,
                    onPost = createViewModel::post
                )
            }

            postId != null -> {
                val detailViewModel: PostDetailViewModel = viewModel(
                    key = "post_detail_${uid}_$postId",
                    factory = remember(uid, postId) {
                        CommunityDependencies.postDetailViewModelFactory(uid, postId)
                    }
                )
                val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()

                // Màn hình rời khỏi cây giao diện khi có màn khác che lên, nên mỗi lần quay lại sẽ tải mới.
                LaunchedEffect(postId) { detailViewModel.load() }

                LaunchedEffect(detailState.errorMessage) {
                    detailState.errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        detailViewModel.consumeError()
                    }
                }

                val closeDetail = {
                    val wasLast = navStack.size <= 1
                    pop()
                    if (wasLast) feedViewModel.refresh() // số like/bình luận có thể đã đổi
                }
                BackHandler { closeDetail() }

                // Xóa bài xong thì tự đóng màn chi tiết.
                LaunchedEffect(detailState.finished) {
                    if (detailState.finished) closeDetail()
                }

                PostDetailScreen(
                    uiState = detailState,
                    currentUid = uid,
                    onBack = closeDetail,
                    onPostLike = detailViewModel::togglePostLike,
                    onFollow = detailViewModel::toggleFollow,
                    onDeletePost = detailViewModel::deletePost,
                    onCommentLike = detailViewModel::toggleCommentLike,
                    onReply = detailViewModel::startReply,
                    onToggleThread = detailViewModel::toggleThread,
                    onCancelReply = detailViewModel::cancelReply,
                    onInputChange = detailViewModel::onInputChange,
                    onSend = detailViewModel::send,
                    onAuthorClick = { push("profile:$it") }
                )
            }

            profileId != null -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    key = "community_profile_${uid}_$profileId",
                    factory = remember(uid, profileId) {
                        CommunityDependencies.profileViewModelFactory(uid, profileId)
                    }
                )
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(profileId) { profileViewModel.load() }

                LaunchedEffect(profileState.errorMessage) {
                    profileState.errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        profileViewModel.consumeError()
                    }
                }

                val closeProfile = {
                    val wasLast = navStack.size <= 1
                    pop()
                    if (wasLast) feedViewModel.refresh() // có thể vừa theo dõi hoặc bỏ theo dõi
                }
                BackHandler { closeProfile() }

                ProfileScreen(
                    uiState = profileState,
                    currentUid = uid,
                    onBack = closeProfile,
                    onRefresh = profileViewModel::load,
                    onFollowClick = profileViewModel::toggleFollow,
                    onLikeClick = profileViewModel::toggleLike,
                    onLoadMore = profileViewModel::loadMore,
                    onPostClick = { push("post:$it") },
                    onDeleteClick = profileViewModel::deletePost
                )
            }

            showSearch -> {
                val searchViewModel: SearchViewModel = viewModel(
                    key = "community_search_$uid",
                    factory = remember(uid) { CommunityDependencies.searchViewModelFactory(uid) }
                )
                val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(searchState.errorMessage) {
                    searchState.errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        searchViewModel.consumeError()
                    }
                }

                val closeSearch = {
                    searchViewModel.onQueryChange("")
                    showSearch = false
                    feedViewModel.refresh() // có thể vừa theo dõi thêm người mới
                }
                BackHandler { closeSearch() }

                SearchScreen(
                    uiState = searchState,
                    onBack = closeSearch,
                    onQueryChange = searchViewModel::onQueryChange,
                    onFollowClick = searchViewModel::toggleFollow,
                    onUserClick = { push("profile:$it") }
                )
            }

            else -> CommunityFeedScreen(
                uiState = feedState,
                currentUid = uid,
                onTabSelected = feedViewModel::selectTab,
                onLikeClick = feedViewModel::toggleLike,
                onFollowClick = feedViewModel::toggleFollow,
                onLoadMore = feedViewModel::loadMore,
                onCreateClick = { showCreate = true },
                onRefresh = feedViewModel::refresh,
                onPostClick = { push("post:$it") },
                onDeleteClick = feedViewModel::deletePost,
                onSearchClick = { showSearch = true },
                onAuthorClick = { push("profile:$it") },
                onMyProfileClick = { push("profile:$uid") }
            )
        }
    }

    val rType = reportType
    val rId = reportTargetId
    if (rType != null && rId != null) {
        ReportDialog(
            targetType = rType,
            isSubmitting = reportState.isSubmitting,
            onDismiss = {
                reportType = null
                reportTargetId = null
            },
            onSubmit = { reason, description, evidence ->
                reportViewModel.submit(rType, rId, reason, description, evidence)
            }
        )
    }
}