package com.example.sweetbean.ui.community.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.community.Post
import com.example.sweetbean.data.community.PostAuthor
import com.example.sweetbean.data.community.PublicProfile
import com.example.sweetbean.ui.community.feed.FeedItem
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val profile: PublicProfile? = null,
    val isOwn: Boolean = false,
    val isFollowing: Boolean = false,
    val items: List<FeedItem> = emptyList(),
    val isLoadingPosts: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val uid: String,
    private val profileId: String,
    private val repository: CommunityRepository,
    private val api: CommunityApi
) : ViewModel() {

    private companion object {
        const val TAG = "CommunityProfile"
    }

    private val _uiState = MutableStateFlow(ProfileUiState(isOwn = uid == profileId))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var cursor: DocumentSnapshot? = null
    private var followBusy = false
    private val likeBusy = mutableSetOf<String>()

    /** Gọi mỗi lần mở màn hình; nếu đã có nội dung cũ thì giữ nguyên trong lúc tải lại. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.profile == null, notFound = false) }
            try {
                val (profile, following) = coroutineScope {
                    val p = async { repository.getPublicProfile(profileId) }
                    val f = async { uid != profileId && repository.isFollowing(uid, profileId) }
                    p.await() to f.await()
                }
                if (profile == null) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                _uiState.update { it.copy(isLoading = false, profile = profile, isFollowing = following) }
                loadPosts(reset = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "load failed", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = messageFor(e)) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingPosts || state.isLoadingMore || state.endReached || state.profile == null) return
        viewModelScope.launch { loadPosts(reset = false) }
    }

    fun toggleFollow() {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.isOwn || followBusy) return

        followBusy = true
        val before = state.isFollowing
        val delta = if (before) -1 else 1
        _uiState.update {
            it.copy(
                isFollowing = !before,
                profile = profile.copy(followersCount = (profile.followersCount + delta).coerceAtLeast(0))
            )
        }
        viewModelScope.launch {
            try {
                repository.toggleFollow(uid, profileId)
                loadPosts(reset = true) // quyền xem bài thay đổi theo quan hệ theo dõi
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleFollow failed", e)
                _uiState.update {
                    it.copy(
                        isFollowing = before,
                        profile = profile,
                        errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại."
                    )
                }
            } finally {
                followBusy = false
            }
        }
    }

    fun toggleLike(postId: String) {
        val current = _uiState.value.items.firstOrNull { it.post.id == postId } ?: return
        if (!likeBusy.add(postId)) return

        setLiked(postId, !current.isLiked)
        viewModelScope.launch {
            try {
                setLiked(postId, repository.toggleLike(uid, postId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleLike failed", e)
                setLiked(postId, current.isLiked)
                _uiState.update { it.copy(errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại.") }
            } finally {
                likeBusy.remove(postId)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                api.deletePost(postId)
                removeItem(postId)
                _uiState.update { state ->
                    state.copy(
                        profile = state.profile?.let {
                            it.copy(postsCount = (it.postsCount - 1).coerceAtLeast(0))
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deletePost failed", e)
                _uiState.update { it.copy(errorMessage = "Không xóa được bài viết. Vui lòng thử lại.") }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ----- Nội bộ -----

    private suspend fun loadPosts(reset: Boolean) {
        if (reset) {
            cursor = null
            _uiState.update { it.copy(isLoadingPosts = true) }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }

        try {
            val page = repository.loadUserPostsPage(
                viewerId = uid,
                ownerId = profileId,
                viewerFollows = _uiState.value.isFollowing,
                cursor = cursor
            )
            cursor = page.cursor ?: cursor
            val items = buildItems(page.posts)

            _uiState.update { state ->
                val merged = if (reset) {
                    items
                } else {
                    state.items + items.filter { n -> state.items.none { it.post.id == n.post.id } }
                }
                state.copy(
                    items = merged,
                    isLoadingPosts = false,
                    isLoadingMore = false,
                    endReached = page.endReached
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "loadPosts failed", e)
            _uiState.update {
                it.copy(isLoadingPosts = false, isLoadingMore = false, errorMessage = messageFor(e))
            }
        }
    }

    private suspend fun buildItems(posts: List<Post>): List<FeedItem> {
        val profile = _uiState.value.profile ?: return emptyList()
        if (posts.isEmpty()) return emptyList()
        val author = PostAuthor(profile.id, profile.displayName, profile.username, profile.avatarUrl)
        val liked = repository.loadLikedPostIds(uid, posts.map { it.id })
        return posts.map { FeedItem(it, author, it.id in liked) }
    }

    private fun setLiked(postId: String, liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.post.id != postId || item.isLiked == liked) {
                        item
                    } else {
                        val delta = if (liked) 1 else -1
                        item.copy(
                            isLiked = liked,
                            post = item.post.copy(likeCount = (item.post.likeCount + delta).coerceAtLeast(0))
                        )
                    }
                }
            )
        }
    }

    private fun removeItem(postId: String) {
        _uiState.update { state -> state.copy(items = state.items.filterNot { it.post.id == postId }) }
    }

    private fun messageFor(e: Throwable): String =
        when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Firestore thiếu chỉ mục cho trang cá nhân (xem Logcat, tag CommunityProfile)."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không có quyền đọc dữ liệu (kiểm tra Firestore Rules)."
            else -> "Không tải được trang cá nhân. Vui lòng thử lại."
        }
}