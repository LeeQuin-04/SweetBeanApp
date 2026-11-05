package com.example.sweetbean.ui.community.create

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityApiException
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.community.PostAuthor
import com.example.sweetbean.data.community.PostLimits
import com.example.sweetbean.data.community.PostVisibility
import com.example.sweetbean.data.journal.JournalMedia
import com.example.sweetbean.data.media.CloudinaryMediaUploader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class PickedMedia(val uri: Uri, val isVideo: Boolean) {
    val key: String get() = uri.toString()
}

data class CreatePostUiState(
    val author: PostAuthor? = null,
    val text: String = "",
    val visibility: String = PostVisibility.PUBLIC,
    val media: List<PickedMedia> = emptyList(),
    val isPosting: Boolean = false,
    val postedLevel: String? = null,
    val errorMessage: String? = null,
    val moderationReviewStatus: String? =null
) {
    val canPost: Boolean get() = !isPosting && (text.isNotBlank() || media.isNotEmpty())
}

class CreatePostViewModel(
    private val uid: String,
    private val repository: CommunityRepository,
    private val api: CommunityApi,
    private val uploader: CloudinaryMediaUploader
) : ViewModel() {

    private companion object {
        const val TAG = "CreatePost"
    }

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    // Đăng lỗi ở bước gọi Worker rồi thử lại thì không upload lại ảnh đã lên Cloudinary.
    private val uploaded = mutableMapOf<String, JournalMedia>()

    init {
        viewModelScope.launch {
            try {
                val author = repository.loadAuthors(listOf(uid))[uid]
                _uiState.update { it.copy(author = author) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "load author failed", e)
            }
        }
    }

    fun onTextChange(value: String) {
        _uiState.update { it.copy(text = value.take(PostLimits.MAX_TEXT_LENGTH)) }
    }

    fun onVisibilityChange(value: String) {
        _uiState.update { it.copy(visibility = value) }
    }

    fun addMedia(uris: List<Uri>, isVideo: Boolean) {
        _uiState.update { state ->
            val existing = state.media.map { it.key }.toSet()
            val fresh = uris.filter { it.toString() !in existing }
            val room = (PostLimits.MAX_MEDIA - state.media.size).coerceAtLeast(0)

            state.copy(
                media = state.media + fresh.take(room).map { PickedMedia(it, isVideo) },
                errorMessage = if (fresh.size > room) {
                    "Mỗi bài viết tối đa ${PostLimits.MAX_MEDIA} ảnh/video."
                } else {
                    state.errorMessage
                }
            )
        }
    }

    fun removeMedia(key: String) {
        _uiState.update { state -> state.copy(media = state.media.filterNot { it.key == key }) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Gọi sau khi đã xử lý xong kết quả đăng bài để form về trạng thái trống. */
    fun reset() {
        uploaded.clear()
        _uiState.update { CreatePostUiState(author = it.author) }
    }

    fun post() {
        val state = _uiState.value
        if (!state.canPost) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, errorMessage = null) }
            try {
                val media = coroutineScope {
                    state.media.map { picked ->
                        async {
                            uploaded[picked.key]
                                ?: uploader.upload(picked.uri, "posts/$uid").also { uploaded[picked.key] = it }
                        }
                    }.awaitAll()
                }
                val result = api.createPost(state.text.trim(), state.visibility, media)
                _uiState.update { it.copy(isPosting = false, postedLevel = result.distributionLevel, moderationReviewStatus = result.moderationReviewStatus) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CommunityApiException) {
                Log.e(TAG, "createPost rejected: ${e.message}")
                _uiState.update { it.copy(isPosting = false, errorMessage = messageFor(e)) }
            } catch (e: Exception) {
                Log.e(TAG, "createPost failed", e)
                _uiState.update {
                    it.copy(
                        isPosting = false,
                        errorMessage = "Không đăng được bài viết. Kiểm tra kết nối rồi thử lại."
                    )
                }
            }
        }
    }

    private fun messageFor(e: CommunityApiException): String = when {
        e.code == 401 -> "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
        e.error == "account_restricted" -> "Tài khoản của bạn đang bị hạn chế, chưa thể đăng bài."
        e.error == "username_required" -> "Hãy thiết lập username trước khi đăng bài."
        e.error == "text_too_long" -> "Nội dung quá dài (tối đa ${PostLimits.MAX_TEXT_LENGTH} ký tự)."
        e.error == "empty_post" -> "Bài viết cần có nội dung hoặc ảnh/video."
        else -> "Không đăng được bài viết. Vui lòng thử lại."
    }
}