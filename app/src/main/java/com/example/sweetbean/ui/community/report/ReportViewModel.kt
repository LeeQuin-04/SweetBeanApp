package com.example.sweetbean.ui.community.report

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityApiException
import com.example.sweetbean.data.community.ReportReason
import com.example.sweetbean.data.media.CloudinaryMediaUploader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val errorMessage: String? = null
)

class ReportViewModel(
    private val uid: String,
    private val api: CommunityApi,
    private val uploader: CloudinaryMediaUploader
) : ViewModel() {

    private companion object {
        const val TAG = "Report"
    }

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    // Ảnh đã upload thành công: gửi báo cáo lỗi rồi thử lại thì không upload lại.
    private val uploaded = mutableMapOf<String, String>()

    fun submit(
        targetType: String,
        targetId: String,
        reason: ReportReason,
        description: String,
        evidence: List<Uri>
    ) {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val urls = coroutineScope {
                    evidence.map { uri ->
                        async {
                            uploaded[uri.toString()]
                                ?: uploader.upload(uri, "reports/$uid").url
                                    .also { uploaded[uri.toString()] = it }
                        }
                    }.awaitAll()
                }
                api.createReport(targetType, targetId, reason.code, description, urls)
                _uiState.update { it.copy(isSubmitting = false, submitted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CommunityApiException) {
                Log.e(TAG, "createReport rejected: ${e.message}")
                _uiState.update { it.copy(isSubmitting = false, errorMessage = messageFor(e)) }
            } catch (e: Exception) {
                Log.e(TAG, "createReport failed", e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Không gửi được báo cáo. Kiểm tra kết nối rồi thử lại."
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        uploaded.clear()
        _uiState.value = ReportUiState()
    }

    private fun messageFor(e: CommunityApiException): String = when {
        e.code == 401 -> "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
        e.error == "already_reported" -> "Bạn đã báo cáo nội dung này rồi."
        e.error == "cannot_report_self" -> "Bạn không thể báo cáo chính mình."
        e.error == "target_not_found" -> "Nội dung không còn tồn tại hoặc bạn không thể báo cáo nội dung này."
        e.error == "account_restricted" -> "Tài khoản của bạn đang bị hạn chế, chưa thể gửi báo cáo."
        e.error == "description_too_long" -> "Mô tả quá dài."
        e.error == "bad_evidence" -> "Ảnh bằng chứng không hợp lệ. Hãy chọn lại ảnh khác."
        else -> "Không gửi được báo cáo. Vui lòng thử lại."
    }
}