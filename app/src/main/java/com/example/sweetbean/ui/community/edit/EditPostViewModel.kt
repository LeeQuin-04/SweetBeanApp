package com.example.sweetbean.ui.community.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityApiException
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.community.Post
import com.example.sweetbean.data.community.PostAuthor
import com.example.sweetbean.data.community.PostLimits
import com.example.sweetbean.data.community.PostVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditPostUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val post: Post? = null,
    val author: PostAuthor? = null,
    val text: String = "",
    val visibility: String = PostVisibility.PUBLIC,
    val isSaving: Boolean = false,
    val savedLevel: String? = null,
    val errorMessage: String? = null,
    val moderationReviewStatus: String? = null
) {
    val hasChanges: Boolean
        get() {
            val p = post ?: return false
            return text.trim() != p.text.trim() || visibility != p.visibility
        }

    val canSave: Boolean
        get() {
            val p = post ?: return false
            return hasChanges && !isSaving && (text.isNotBlank() || p.media.isNotEmpty())
        }
}

class EditPostViewModel(
    private val uid: String,
    private val postId: String,
    private val repository: CommunityRepository,
    private val api: CommunityApi
) : ViewModel() {

    private companion object {
        const val TAG = "EditPost"
    }

    private val _uiState = MutableStateFlow(EditPostUiState())
    val uiState: StateFlow<EditPostUiState> = _uiState.asStateFlow()

    /** Gọi mỗi lần mở màn hình: nạp lại nội dung hiện tại của bài. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = EditPostUiState(isLoading = true)
            try {
                val post = repository.getPost(postId)
                if (post == null || post.authorId != uid) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                val author = repository.loadAuthors(listOf(uid))[uid]
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        post = post,
                        author = author,
                        text = post.text,
                        visibility = post.visibility
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "load failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notFound = true,
                        errorMessage = "Không tải được bài viết. Vui lòng thử lại."
                    )
                }
            }
        }
    }

    fun onTextChange(value: String) {
        _uiState.update { it.copy(text = value.take(PostLimits.MAX_TEXT_LENGTH)) }
    }

    fun onVisibilityChange(value: String) {
        _uiState.update { it.copy(visibility = value) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val result = api.editPost(postId, state.text.trim(), state.visibility)
                _uiState.update { it.copy(isSaving = false, savedLevel = result.distributionLevel, moderationReviewStatus = result.moderationReviewStatus) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CommunityApiException) {
                Log.e(TAG, "editPost rejected: ${e.message}")
                _uiState.update { it.copy(isSaving = false, errorMessage = messageFor(e)) }
            } catch (e: Exception) {
                Log.e(TAG, "editPost failed", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Không lưu được thay đổi. Kiểm tra kết nối rồi thử lại."
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Gọi sau khi đã xử lý kết quả lưu để tránh xử lý lại. */
    fun reset() {
        _uiState.update { it.copy(savedLevel = null, moderationReviewStatus = null) }
    }

    private fun messageFor(e: CommunityApiException): String = when {
        e.code == 401 -> "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
        e.error == "post_locked" -> "Bài viết đang chờ quản trị viên xem xét nên chưa thể chỉnh sửa."
        e.error == "forbidden" -> "Bạn không thể chỉnh sửa bài viết này."
        e.error == "post_not_found" -> "Bài viết không còn tồn tại."
        e.error == "account_restricted" -> "Tài khoản của bạn đang bị hạn chế, chưa thể chỉnh sửa."
        e.error == "text_too_long" -> "Nội dung quá dài (tối đa ${PostLimits.MAX_TEXT_LENGTH} ký tự)."
        e.error == "empty_post" -> "Bài viết cần có nội dung hoặc ảnh/video."
        else -> "Không lưu được thay đổi. Vui lòng thử lại."
    }
}