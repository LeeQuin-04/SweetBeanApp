package com.example.sweetbean.ui.community.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.Comment
import com.example.sweetbean.data.community.CommentLimits
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityApiException
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.community.PostAuthor
import com.example.sweetbean.data.community.Post
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentItem(
    val comment: Comment,
    val author: PostAuthor?,
    val isLiked: Boolean
)

data class CommentThread(
    val root: CommentItem,
    val replies: List<CommentItem>
)

data class PostDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val post: Post? = null,
    val author: PostAuthor? = null,
    val isPostLiked: Boolean = false,
    val isFollowingAuthor: Boolean = false,
    val threads: List<CommentThread> = emptyList(),
    val expandedThreads: Set<String> = emptySet(),
    val replyingTo: CommentItem? = null,
    val input: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val replyTarget: CommentItem? = null,
    val finished: Boolean = false
)

class PostDetailViewModel(
    private val uid: String,
    private val postId: String,
    private val repository: CommunityRepository,
    private val api: CommunityApi
) : ViewModel() {

    private companion object {
        const val TAG = "PostDetail"
    }

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var followingIds: Set<String> = emptySet()
    private var followBusy = false
    private val likeBusy = mutableSetOf<String>()

    /** Gọi mỗi lần mở màn hình; nếu đã có nội dung cũ thì giữ nguyên trong lúc tải lại. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.post == null, notFound = false, finished = false) }
            try {
                val post = repository.getPost(postId)
                if (post == null) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }

                val (author, liked, following) = coroutineScope {
                    val a = async { repository.loadAuthors(listOf(post.authorId))[post.authorId] }
                    val l = async { repository.loadLikedPostIds(uid, listOf(post.id)).isNotEmpty() }
                    val f = async { repository.loadFollowingIds(uid) }
                    Triple(a.await(), l.await(), f.await())
                }
                followingIds = following

                val threads = buildThreads()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        post = post,
                        author = author,
                        isPostLiked = liked,
                        isFollowingAuthor = post.authorId in following,
                        threads = threads
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "load failed", e)
                val denied = (e as? FirebaseFirestoreException)?.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notFound = denied && it.post == null,
                        errorMessage = messageFor(e)
                    )
                }
            }
        }
    }

    fun togglePostLike() {
        val state = _uiState.value
        val post = state.post ?: return
        if (!likeBusy.add(post.id)) return

        val before = state.isPostLiked
        setPostLiked(!before)
        viewModelScope.launch {
            try {
                setPostLiked(repository.toggleLike(uid, post.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleLike failed", e)
                setPostLiked(before)
                _uiState.update { it.copy(errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại.") }
            } finally {
                likeBusy.remove(post.id)
            }
        }
    }

    fun toggleFollow() {
        val post = _uiState.value.post ?: return
        if (post.authorId == uid || followBusy) return

        followBusy = true
        val before = _uiState.value.isFollowingAuthor
        _uiState.update { it.copy(isFollowingAuthor = !before) }
        viewModelScope.launch {
            try {
                repository.toggleFollow(uid, post.authorId)
                followingIds = if (before) followingIds - post.authorId else followingIds + post.authorId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleFollow failed", e)
                _uiState.update {
                    it.copy(
                        isFollowingAuthor = before,
                        errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại."
                    )
                }
            } finally {
                followBusy = false
            }
        }
    }

    fun toggleCommentLike(commentId: String) {
        val current = _uiState.value.threads
            .asSequence()
            .flatMap { sequenceOf(it.root) + it.replies.asSequence() }
            .firstOrNull { it.comment.id == commentId } ?: return
        if (!likeBusy.add(commentId)) return

        setCommentLiked(commentId, !current.isLiked)
        viewModelScope.launch {
            try {
                setCommentLiked(commentId, repository.toggleCommentLike(uid, commentId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleCommentLike failed", e)
                setCommentLiked(commentId, current.isLiked)
                _uiState.update { it.copy(errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại.") }
            } finally {
                likeBusy.remove(commentId)
            }
        }
    }

    /** Trả lời luôn gắn vào bình luận gốc; trả lời một reply thì điền sẵn @username. */
    fun startReply(root: CommentItem, target: CommentItem) {
        val mention = target.author?.username?.takeIf { it.isNotBlank() && target !== root }
        _uiState.update {
            it.copy(
                replyingTo = root,
                replyTarget = target,
                input = if (mention != null) "@$mention " else it.input
            )
        }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingTo = null, replyTarget = null) }
    }

    fun toggleThread(rootId: String) {
        _uiState.update {
            it.copy(
                expandedThreads = if (rootId in it.expandedThreads) it.expandedThreads - rootId
                else it.expandedThreads + rootId
            )
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value.take(CommentLimits.MAX_TEXT_LENGTH)) }
    }

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        if (text.isEmpty() || state.isSending) return

        val parentId = state.replyingTo?.comment?.id
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            // Bước 1: gửi bình luận qua Worker.
            try {
                api.createComment(postId, text, parentId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "createComment failed", e)
                _uiState.update { it.copy(isSending = false, errorMessage = sendErrorMessage(e)) }
                return@launch
            }

            // Bước 2: bình luận đã lưu, xóa ô nhập rồi mới tải lại danh sách.
            _uiState.update {
                it.copy(
                    isSending = false,
                    input = "",
                    replyingTo = null,
                    replyTarget = null,
                    expandedThreads = if (parentId != null) it.expandedThreads + parentId
                    else it.expandedThreads
                )
            }
            try {
                val post = repository.getPost(postId)
                val threads = buildThreads()
                _uiState.update { it.copy(post = post ?: it.post, threads = threads) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "reload after send failed", e)
                _uiState.update {
                    it.copy(errorMessage = "Đã gửi bình luận nhưng chưa tải lại được danh sách. Hãy mở lại bài viết để xem.")
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Quy tắc hiển thị bình luận theo nhãn. */
    private suspend fun buildThreads(): List<CommentThread> {
        val visible = repository.loadComments(postId, uid).filter { c ->
            when (c.moderationLabel) {
                "CLEAN" -> true
                "OFFENSIVE" -> c.authorId == uid || c.authorId in followingIds
                else -> c.authorId == uid // HATE hoặc chưa kiểm duyệt: chỉ tác giả
            }
        }
        if (visible.isEmpty()) return emptyList()

        val (authors, liked) = coroutineScope {
            val a = async { repository.loadAuthors(visible.map { it.authorId }) }
            val l = async { repository.loadLikedCommentIds(uid, visible.map { it.id }) }
            a.await() to l.await()
        }

        fun item(c: Comment) = CommentItem(c, authors[c.authorId], c.id in liked)

        val repliesByParent = visible.filter { it.parentCommentId != null }.groupBy { it.parentCommentId!! }
        return visible
            .filter { it.parentCommentId == null }
            .map { root -> CommentThread(item(root), repliesByParent[root.id].orEmpty().map(::item)) }
    }

    private fun setPostLiked(liked: Boolean) {
        _uiState.update { state ->
            val post = state.post
            if (post == null || state.isPostLiked == liked) {
                state
            } else {
                val delta = if (liked) 1 else -1
                state.copy(
                    isPostLiked = liked,
                    post = post.copy(likeCount = (post.likeCount + delta).coerceAtLeast(0))
                )
            }
        }
    }

    private fun setCommentLiked(commentId: String, liked: Boolean) {
        fun update(item: CommentItem): CommentItem {
            if (item.comment.id != commentId || item.isLiked == liked) return item
            val delta = if (liked) 1 else -1
            return item.copy(
                isLiked = liked,
                comment = item.comment.copy(likeCount = (item.comment.likeCount + delta).coerceAtLeast(0))
            )
        }

        _uiState.update { state ->
            state.copy(
                threads = state.threads.map { t ->
                    CommentThread(update(t.root), t.replies.map(::update))
                }
            )
        }
    }

    private fun messageFor(e: Throwable): String {
        if (e is CommunityApiException) {
            return when {
                e.code == 401 -> "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
                e.error == "account_restricted" -> "Tài khoản của bạn đang bị hạn chế, chưa thể bình luận."
                e.error == "username_required" -> "Hãy thiết lập username trước khi bình luận."
                e.error == "text_too_long" -> "Bình luận quá dài (tối đa ${CommentLimits.MAX_TEXT_LENGTH} ký tự)."
                e.error == "post_not_found" || e.error == "post_not_visible" ->
                    "Bạn không thể bình luận bài viết này."
                e.error == "parent_not_found" -> "Bình luận bạn muốn trả lời không còn tồn tại."
                else -> "Không gửi được bình luận. Vui lòng thử lại."
            }
        }
        return when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Firestore thiếu chỉ mục cho bình luận (xem Logcat, tag PostDetail)."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không có quyền đọc dữ liệu (kiểm tra Firestore Rules)."
            else -> "Không tải được dữ liệu. Kiểm tra kết nối rồi thử lại."
        }
    }
    private fun sendErrorMessage(e: Throwable): String =
        if (e is CommunityApiException) messageFor(e)
        else "Không gửi được bình luận. Kiểm tra kết nối rồi thử lại."

    fun deletePost() {
        val post = _uiState.value.post ?: return
        if (post.authorId != uid) return
        viewModelScope.launch {
            try {
                api.deletePost(post.id)
                _uiState.update { it.copy(finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deletePost failed", e)
                _uiState.update { it.copy(errorMessage = "Không xóa được bài viết. Vui lòng thử lại.") }
            }
        }
    }

    fun hidePost() {
        val post = _uiState.value.post ?: return
        viewModelScope.launch {
            try {
                repository.hidePost(uid, post.id)
                _uiState.update { it.copy(finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "hidePost failed", e)
                _uiState.update { it.copy(errorMessage = "Không ẩn được bài viết. Vui lòng thử lại.") }
            }
        }
    }
}