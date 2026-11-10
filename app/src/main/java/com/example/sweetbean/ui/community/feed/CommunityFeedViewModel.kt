package com.example.sweetbean.ui.community.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.community.Post
import com.example.sweetbean.data.community.PostAuthor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FeedTab(val label: String) {
    FOLLOWING("Đang theo dõi"),
    ALL("Khám phá")
}

data class FeedItem(
    val post: Post,
    val author: PostAuthor?,
    val isLiked: Boolean
)

/** Mỗi tab giữ danh sách riêng để vuốt qua lại không phải tải lại. */
data class FeedTabState(
    val items: List<FeedItem> = emptyList(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val failed: Boolean = false
)

data class CommunityFeedUiState(
    val tab: FeedTab = FeedTab.FOLLOWING,
    val tabs: Map<FeedTab, FeedTabState> = FeedTab.entries.associateWith { FeedTabState() },
    val followingIds: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    fun tabState(tab: FeedTab): FeedTabState = tabs.getValue(tab)
}

class CommunityFeedViewModel(
    private val uid: String,
    private val repository: CommunityRepository,
    private val api: CommunityApi
) : ViewModel() {

    private companion object {
        const val TAG = "CommunityFeed"
    }

    private val _uiState = MutableStateFlow(CommunityFeedUiState())
    val uiState: StateFlow<CommunityFeedUiState> = _uiState.asStateFlow()

    private var followingReady = false
    private var allCursor: DocumentSnapshot? = null
    private val loadJobs = mutableMapOf<FeedTab, Job>()
    private val likeBusy = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.observeFollowingIds(uid)
                .catch { e ->
                    Log.e(TAG, "observeFollowingIds failed", e)
                    fail(null, e)
                }
                .collect { ids ->
                    val first = !followingReady
                    followingReady = true
                    _uiState.update { it.copy(followingIds = ids) }
                    // Chỉ tải ở lần đầu; các lần sau chỉ đổi trạng thái nút Theo dõi.
                    if (first) load(FeedTab.FOLLOWING)
                }
        }
        // Tải sẵn tab Khám phá để vuốt sang là có nội dung.
        ensureLoaded(FeedTab.ALL)
    }

    fun selectTab(tab: FeedTab) {
        if (tab != _uiState.value.tab) _uiState.update { it.copy(tab = tab) }
        ensureLoaded(tab)
    }

    /** Tải lại tab đang xem; các tab khác đánh dấu cũ và tự tải lại khi người dùng vuốt tới. */
    fun refresh() {
        val current = _uiState.value.tab
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.mapValues { (tab, tabState) ->
                    if (tab == current) tabState else tabState.copy(loaded = false)
                }
            )
        }
        load(current)
    }

    fun loadMore() {
        val state = _uiState.value
        val all = state.tabState(FeedTab.ALL)
        if (state.tab != FeedTab.ALL || !all.loaded || all.isLoading || all.isLoadingMore || all.endReached) return

        viewModelScope.launch {
            updateTab(FeedTab.ALL) { it.copy(isLoadingMore = true) }
            try {
                val page = repository.loadAllPage(allCursor)
                allCursor = page.cursor ?: allCursor
                val newItems = buildItems(page.posts)
                updateTab(FeedTab.ALL) { tabState ->
                    val known = tabState.items.map { it.post.id }.toSet()
                    tabState.copy(
                        items = tabState.items + newItems.filter { it.post.id !in known },
                        isLoadingMore = false,
                        endReached = page.endReached
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "loadMore failed", e)
                updateTab(FeedTab.ALL) { it.copy(isLoadingMore = false) }
                _uiState.update { it.copy(errorMessage = messageFor(e)) }
            }
        }
    }

    fun toggleLike(postId: String) {
        val current = _uiState.value.tabs.values
            .asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.post.id == postId } ?: return
        if (!likeBusy.add(postId)) return

        setLiked(postId, !current.isLiked) // cập nhật ngay, lỗi thì hoàn lại
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

    fun toggleFollow(authorId: String) {
        if (authorId == uid) return
        viewModelScope.launch {
            try {
                repository.toggleFollow(uid, authorId) // nút tự đổi nhờ snapshot listener
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleFollow failed", e)
                _uiState.update { it.copy(errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại.") }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                api.deletePost(postId)
                removeItem(postId)
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

    private fun ensureLoaded(tab: FeedTab) {
        val tabState = _uiState.value.tabState(tab)
        if (tabState.loaded || tabState.isLoading) return
        load(tab)
    }

    private fun load(tab: FeedTab) {
        if (tab == FeedTab.FOLLOWING && !followingReady) {
            // Đợi danh sách đang theo dõi về rồi collector ở init sẽ gọi lại.
            updateTab(tab) { it.copy(isLoading = true) }
            return
        }

        loadJobs[tab]?.cancel()
        loadJobs[tab] = viewModelScope.launch {
            updateTab(tab) { it.copy(isLoading = true, isLoadingMore = false, failed = false) }
            try {
                val posts: List<Post>
                var endReached = true

                if (tab == FeedTab.ALL) {
                    val page = repository.loadAllPage(null)
                    allCursor = page.cursor
                    endReached = page.endReached
                    posts = page.posts
                } else {
                    posts = repository.loadFollowingFeed(_uiState.value.followingIds)
                }

                val items = buildItems(posts)
                updateTab(tab) {
                    it.copy(
                        items = items,
                        loaded = true,
                        isLoading = false,
                        endReached = endReached,
                        failed = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "load($tab) failed", e)
                fail(tab, e)
            }
        }
    }

    private suspend fun buildItems(posts: List<Post>): List<FeedItem> {
        if (posts.isEmpty()) return emptyList()
        val authors = repository.loadAuthors(posts.map { it.authorId })
        val liked = repository.loadLikedPostIds(uid, posts.map { it.id })
        return posts.map { FeedItem(it, authors[it.authorId], it.id in liked) }
    }

    private fun updateTab(tab: FeedTab, transform: (FeedTabState) -> FeedTabState) {
        _uiState.update { state ->
            state.copy(tabs = state.tabs + (tab to transform(state.tabs.getValue(tab))))
        }
    }

    private fun setLiked(postId: String, liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.mapValues { (_, tabState) ->
                    tabState.copy(
                        items = tabState.items.map { item ->
                            if (item.post.id != postId || item.isLiked == liked) {
                                item
                            } else {
                                val delta = if (liked) 1 else -1
                                item.copy(
                                    isLiked = liked,
                                    post = item.post.copy(
                                        likeCount = (item.post.likeCount + delta).coerceAtLeast(0)
                                    )
                                )
                            }
                        }
                    )
                }
            )
        }
    }

    private fun removeItem(postId: String) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.mapValues { (_, tabState) ->
                    tabState.copy(items = tabState.items.filterNot { it.post.id == postId })
                }
            )
        }
    }

    private fun fail(tab: FeedTab?, e: Throwable) {
        val message = messageFor(e)
        _uiState.update { state ->
            val tabs = if (tab == null) {
                state.tabs
            } else {
                state.tabs + (tab to state.tabs.getValue(tab).copy(
                    isLoading = false,
                    isLoadingMore = false,
                    failed = true
                ))
            }
            state.copy(tabs = tabs, errorMessage = message)
        }
    }

    private fun messageFor(e: Throwable): String =
        when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Firestore thiếu chỉ mục cho bảng tin (xem Logcat, tag CommunityFeed)."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không có quyền đọc bài viết (kiểm tra Firestore Rules)."
            else -> "Không tải được bảng tin. Vui lòng thử lại."
        }
}