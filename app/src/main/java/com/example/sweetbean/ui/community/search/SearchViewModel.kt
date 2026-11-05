package com.example.sweetbean.ui.community.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.community.PostAuthor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class SearchUiState(
    val query: String = "",
    val results: List<PostAuthor> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val uid: String,
    private val repository: CommunityRepository
) : ViewModel() {

    private companion object {
        const val TAG = "CommunitySearch"
        const val MIN_QUERY = 2
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    private val followBusy = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.observeFollowingIds(uid)
                .catch { e -> Log.e(TAG, "observeFollowingIds failed", e) }
                .collect { ids -> _uiState.update { it.copy(followingIds = ids) } }
        }
        // Đợi người dùng ngừng gõ 350ms rồi mới tìm; gõ tiếp thì hủy lượt tìm cũ.
        viewModelScope.launch {
            query.debounce(350.milliseconds).distinctUntilChanged().collectLatest { search(it) }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        query.value = value
    }

    fun toggleFollow(targetId: String) {
        if (targetId == uid || !followBusy.add(targetId)) return
        viewModelScope.launch {
            try {
                repository.toggleFollow(uid, targetId) // nút tự đổi nhờ snapshot listener
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleFollow failed", e)
                _uiState.update { it.copy(errorMessage = "Không thực hiện được thao tác. Vui lòng thử lại.") }
            } finally {
                followBusy.remove(targetId)
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun search(raw: String) {
        val q = raw.trim().removePrefix("@")
        if (q.length < MIN_QUERY) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false, hasSearched = false) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }
        try {
            val results = repository.searchUsers(q).filter { it.id != uid }
            _uiState.update { it.copy(results = results, isSearching = false, hasSearched = true) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "search failed", e)
            _uiState.update {
                it.copy(isSearching = false, errorMessage = "Không tìm kiếm được. Vui lòng thử lại.")
            }
        }
    }
}