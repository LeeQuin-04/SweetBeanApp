package com.example.sweetbean.ui.journal.editor

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.journal.Emotion
import com.example.sweetbean.data.journal.Journal
import com.example.sweetbean.data.journal.JournalDraft
import com.example.sweetbean.data.journal.JournalLimits
import com.example.sweetbean.data.journal.JournalMedia
import com.example.sweetbean.data.journal.JournalRepository
import com.example.sweetbean.data.media.CloudinaryMediaUploader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.sweetbean.data.journal.DayKey
import com.example.sweetbean.data.media.MediaCleaner

/** Một ảnh/video trong form: đã có trên Cloudinary (Remote) hoặc mới chọn từ máy (Local). */
sealed interface EditorMedia {
    val key: String
    val isVideo: Boolean

    data class Remote(val media: JournalMedia) : EditorMedia {
        override val key: String get() = media.publicId.ifBlank { media.url }
        override val isVideo: Boolean get() = media.isVideo
    }

    data class Local(val uri: Uri, override val isVideo: Boolean) : EditorMedia {
        override val key: String get() = uri.toString()
    }
}

data class JournalEditorUiState(
    val editingId: String? = null, // null = đang tạo mới
    val emotion: Emotion? = null,
    val title: String = "",
    val text: String = "",
    val media: List<EditorMedia> = emptyList(),
    val isLoading: Boolean = false, // đang kiểm tra hôm nay đã có nhật ký chưa
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val day: DayKey? = null, // ngày đang viết
    val errorMessage: String? = null
)

class JournalEditorViewModel(
    private val uid: String,
    private val repository: JournalRepository,
    private val uploader: CloudinaryMediaUploader,
    private val mediaCleaner: MediaCleaner
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalEditorUiState())
    val uiState: StateFlow<JournalEditorUiState> = _uiState.asStateFlow()
    private var originalMedia: List<JournalMedia> = emptyList() //media gốc của nhật ký đang sửa

    /** Mở form chỉnh sửa một nhật ký đã có. */
    fun start(journal: Journal) {
        if (_uiState.value.isSaving) return
        originalMedia = journal.media
        _uiState.value = journal.toEditorState()
    }

    /**
     * Mở form cho ngày [day]: nếu ngày đó đã có nhật ký thì nạp nó để chỉnh sửa
     * (mỗi ngày một nhật ký), nếu chưa có thì mở form trống.
     */
    fun startFor(day: DayKey) {
        if (_uiState.value.isSaving) return
        originalMedia = emptyList()                        // mặc định là form tạo mới, chưa có media gốc
        _uiState.value = JournalEditorUiState(isLoading = true, day = day)

        viewModelScope.launch {
            try {
                val existing = repository.findDay(uid, day)
                _uiState.value = if (existing == null) {
                    JournalEditorUiState(day = day)
                } else {
                    originalMedia = existing.media         // ngày này đã có nhật ký nên form nạp để sửa
                    existing.toEditorState().copy(
                        errorMessage = "Ngày ${day.label()} đã có nhật ký, đang mở để chỉnh sửa."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("JournalEditor", "findDay failed", e)
                _uiState.value = JournalEditorUiState(
                    day = day,
                    loadFailed = true,
                    errorMessage = "Không kiểm tra được nhật ký của ngày này. Kiểm tra kết nối rồi thử lại."
                )
            }
        }
    }

    fun onEmotionSelected(emotion: Emotion) {
        _uiState.update { it.copy(emotion = emotion) }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value.take(JournalLimits.MAX_TITLE_LENGTH)) }
    }

    fun onTextChange(value: String) {
        _uiState.update { it.copy(text = value.take(JournalLimits.MAX_TEXT_LENGTH)) }
    }

    fun addMedia(uris: List<Uri>) {
        _uiState.update { state ->
            val existing = state.media.map { it.key }.toSet()
            val fresh = uris.filter { it.toString() !in existing }
            val room = (JournalLimits.MAX_MEDIA - state.media.size).coerceAtLeast(0)
            val accepted = fresh.take(room)

            state.copy(
                media = state.media + accepted.map { EditorMedia.Local(it, uploader.isVideo(it)) },
                errorMessage = if (fresh.size > room) {
                    "Mỗi nhật ký tối đa ${JournalLimits.MAX_MEDIA} ảnh/video."
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

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        val emotion = state.emotion
        if (emotion == null) {
            _uiState.update { it.copy(errorMessage = "Hãy chọn cảm xúc của bạn.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val uploaded = mutableListOf<JournalMedia>()   // ảnh vừa tải lên trong lần lưu này

            try {
                // Giữ nguyên thứ tự: ảnh cũ giữ lại, ảnh mới upload lên Cloudinary.
                val media = state.media.map { item ->
                    when (item) {
                        is EditorMedia.Remote -> item.media
                        is EditorMedia.Local ->
                            uploader.upload(item.uri, "journals/$uid").also { uploaded += it }
                    }
                }

                val draft = JournalDraft(
                    title = state.title.trim(),
                    text = state.text.trim(),
                    emotion = emotion,
                    media = media
                )

                val editingId = state.editingId
                if (editingId == null) {
                    repository.create(uid, state.day ?: DayKey.today(), draft)
                } else {
                    repository.update(editingId, draft)
                }

                // Ảnh cũ bị bỏ khỏi nhật ký thì xóa luôn trên Cloudinary.
                val kept = media.map { it.publicId }.toSet()
                mediaCleaner.delete(originalMedia.filter { it.publicId !in kept })
                originalMedia = media

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("JournalEditor", "save failed", e)
                mediaCleaner.delete(uploaded)   // lưu thất bại: dọn ảnh vừa tải lên để không bỏ rơi trên Cloudinary
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Không lưu được nhật ký. Kiểm tra kết nối rồi thử lại."
                    )
                }
            }
        }
    }

    private fun Journal.toEditorState() = JournalEditorUiState(
        editingId = id,
        day = DayKey.of(dateMillis),
        emotion = emotion,
        title = title,
        text = text,
        media = media.map { EditorMedia.Remote(it) }
    )
}