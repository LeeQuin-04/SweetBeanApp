package com.example.sweetbean.ui.journal.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.journal.Journal
import com.example.sweetbean.data.journal.JournalRepository
import com.example.sweetbean.ui.journal.JournalFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.sweetbean.data.journal.DayKey
import com.example.sweetbean.data.media.MediaCleaner

data class JournalDayGroup(
    val label: String,
    val items: List<Journal>
)

data class JournalListUiState(
    val year: Int,
    val month: Int, // 1..12
    val isLoading: Boolean = true,
    val groups: List<JournalDayGroup> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalListViewModel(
    private val uid: String,
    private val repository: JournalRepository,
    private val mediaCleaner: MediaCleaner
) : ViewModel() {

    private val period: MutableStateFlow<Pair<Int, Int>>
    private val _uiState: MutableStateFlow<JournalListUiState>
    val uiState: StateFlow<JournalListUiState>

    init {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1

        period = MutableStateFlow(year to month)
        _uiState = MutableStateFlow(JournalListUiState(year = year, month = month))
        uiState = _uiState.asStateFlow()

        viewModelScope.launch {
            period.flatMapLatest { (y, m) ->
                repository.observeMonth(uid, y, m).catch { e ->
                    if (e is CancellationException) throw e
                    // Lỗi thiếu chỉ mục Firestore sẽ có đường link tạo chỉ mục trong Logcat.
                    Log.e("JournalList", "observeMonth failed", e)
                    val message = when ((e as? FirebaseFirestoreException)?.code) {
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                            "Firestore thiếu chỉ mục cho truy vấn nhật ký (xem Logcat, tag JournalList)."
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            "Không có quyền đọc nhật ký (kiểm tra Firestore Rules)."
                        else -> "Không tải được nhật ký. Vui lòng thử lại."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
            }.collect { journals ->
                _uiState.update {
                    it.copy(isLoading = false, groups = journals.groupByDay())
                }
            }
        }
    }

    fun selectMonth(month: Int) = changePeriod(_uiState.value.year, month)

    fun selectYear(year: Int) = changePeriod(year, _uiState.value.month)

    fun delete(journal: Journal) {
        viewModelScope.launch {
            try {
                repository.delete(journal.id)
                mediaCleaner.delete(journal.media)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("JournalList", "delete failed", e)
                _uiState.update { it.copy(errorMessage = "Không xóa được nhật ký.") }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun changePeriod(year: Int, month: Int) {
        _uiState.update {
            it.copy(year = year, month = month, isLoading = true, groups = emptyList())
        }
        period.value = year to month
    }

    // Danh sách đã sắp mới nhất trước, groupBy giữ nguyên thứ tự đó.
    private fun List<Journal>.groupByDay(): List<JournalDayGroup> =
        groupBy { JournalFormat.day(it.dateMillis) }
            .map { (label, items) -> JournalDayGroup(label, items) }

    /** Nhật ký của một ngày trong tháng đang xem (null nếu chưa có). */
    fun JournalListUiState.journalOn(day: DayKey): Journal? =
        groups.asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { DayKey.of(it.dateMillis) == day }
}