package com.example.sweetbean.ui.journal.tree

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.journal.JournalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class EmotionTreeUiState(
    val year: Int,
    val isLoading: Boolean = true,
    val months: List<MonthSummary> = emptyList(),
    val selectedMonth: Int? = null,
    val errorMessage: String? = null
) {
    val totalEntries: Int get() = months.sumOf { it.total }
    val bloomedMonths: Int get() = months.count { it.total > 0 }
}

class EmotionTreeViewModel(
    private val uid: String,
    private val repository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EmotionTreeUiState(year = Calendar.getInstance().get(Calendar.YEAR))
    )
    val uiState: StateFlow<EmotionTreeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** Gọi mỗi khi mở màn hình, để cây phản ánh các nhật ký vừa viết. */
    fun refresh() = load(_uiState.value.year, keepMonths = true)

    fun selectYear(year: Int) = load(year, keepMonths = false)

    /** Chạm lại đúng tháng đang chọn thì bỏ chọn. */
    fun selectMonth(month: Int?) {
        _uiState.update { it.copy(selectedMonth = if (it.selectedMonth == month) null else month) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun load(year: Int, keepMonths: Boolean) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                year = year,
                isLoading = true,
                months = if (keepMonths) it.months else emptyList(),
                selectedMonth = if (keepMonths) it.selectedMonth else null
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val journals = repository.loadYear(uid, year)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        months = EmotionTreeCalculator.summarize(year, journals)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EmotionTree", "load failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Không tải được cây cảm xúc. Vui lòng thử lại."
                    )
                }
            }
        }
    }
}