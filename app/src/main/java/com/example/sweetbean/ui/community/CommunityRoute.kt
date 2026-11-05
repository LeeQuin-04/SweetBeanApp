package com.example.sweetbean.ui.community

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
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
import com.example.sweetbean.di.AuthDependencies
import com.example.sweetbean.di.CommunityDependencies
import com.example.sweetbean.ui.community.create.CreatePostScreen
import com.example.sweetbean.ui.community.create.CreatePostViewModel
import com.example.sweetbean.ui.community.detail.PostDetailScreen
import com.example.sweetbean.ui.community.detail.PostDetailViewModel
import com.example.sweetbean.ui.community.edit.EditPostScreen
import com.example.sweetbean.ui.community.edit.EditPostViewModel
import com.example.sweetbean.ui.community.feed.CommunityFeedScreen
import com.example.sweetbean.ui.community.feed.CommunityFeedViewModel
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

    val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val createState by createViewModel.uiState.collectAsStateWithLifecycle()

    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var openPostId by rememberSaveable { mutableStateOf<String?>(null) }
    var editPostId by rememberSaveable { mutableStateOf<String?>(null) }

    // Đăng xong: đóng form, báo kết quả, tải lại bảng tin.
    LaunchedEffect(createState.postedLevel) {
        val level = createState.postedLevel ?: return@LaunchedEffect
        showCreate = false
        val message = if (level == DistributionLevel.NORMAL) {
            "Đã đăng bài viết."
        } else if(createState.moderationReviewStatus == "pending_ai_unavailable") {
            "Đang chờ kiểm duyệt"
        }else{
            "Đã đăng bài, nhưng bài viết đang bị giới hạn hiển thị."
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        createViewModel.reset()
        feedViewModel.refresh()
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

    val postId = openPostId
    val editId = editPostId

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

        editId != null -> {
            val editViewModel: EditPostViewModel = viewModel(
                key = "post_edit_${uid}_$editId",
                factory = remember(uid, editId) {
                    CommunityDependencies.editPostViewModelFactory(uid, editId)
                }
            )
            val editState by editViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(editId) { editViewModel.load() }

            LaunchedEffect(editState.errorMessage) {
                editState.errorMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    editViewModel.consumeError()
                }
            }

            // Lưu xong: báo kết quả, về màn trước (bảng tin hoặc chi tiết) và tải lại.
            LaunchedEffect(editState.savedLevel) {
                val level = editState.savedLevel ?: return@LaunchedEffect
                val message = if (level == DistributionLevel.NORMAL) {
                    "Đã lưu thay đổi."
                } else {
                    "Đã lưu, nhưng bài viết đang bị giới hạn hiển thị."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                editPostId = null
                feedViewModel.refresh()
                editViewModel.reset()
            }

            BackHandler(enabled = !editState.isSaving) { editPostId = null }

            EditPostScreen(
                uiState = editState,
                onBack = { editPostId = null },
                onTextChange = editViewModel::onTextChange,
                onVisibilityChange = editViewModel::onVisibilityChange,
                onSave = editViewModel::save
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

            // Mỗi lần mở bài đều tải lại để có số liệu mới nhất.
            LaunchedEffect(postId) { detailViewModel.load() }

            LaunchedEffect(detailState.errorMessage) {
                detailState.errorMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    detailViewModel.consumeError()
                }
            }

            val closeDetail = {
                openPostId = null
                feedViewModel.refresh() // số like/bình luận có thể đã đổi
            }
            BackHandler { closeDetail() }

            // Xóa hoặc ẩn bài xong thì tự đóng màn chi tiết.
            LaunchedEffect(detailState.finished) {
                if (detailState.finished) closeDetail()
            }

            PostDetailScreen(
                uiState = detailState,
                currentUid = uid,
                onBack = closeDetail,
                onPostLike = detailViewModel::togglePostLike,
                onFollow = detailViewModel::toggleFollow,
                onEditPost = { editPostId = postId },
                onDeletePost = detailViewModel::deletePost,
                onHidePost = detailViewModel::hidePost,
                onCommentLike = detailViewModel::toggleCommentLike,
                onReply = detailViewModel::startReply,
                onToggleThread = detailViewModel::toggleThread,
                onCancelReply = detailViewModel::cancelReply,
                onInputChange = detailViewModel::onInputChange,
                onSend = detailViewModel::send
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
                onFollowClick = searchViewModel::toggleFollow
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
            onPostClick = { openPostId = it },
            onDeleteClick = feedViewModel::deletePost,
            onHideClick = feedViewModel::hidePost,
            onEditClick = { editPostId = it },
            onSearchClick = { showSearch = true }
        )
    }
}