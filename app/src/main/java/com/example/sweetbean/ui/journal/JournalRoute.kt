package com.example.sweetbean.ui.journal

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sweetbean.di.AuthDependencies
import com.example.sweetbean.di.JournalDependencies
import com.example.sweetbean.ui.journal.detail.JournalDetailDialog
import com.example.sweetbean.ui.journal.editor.JournalEditorScreen
import com.example.sweetbean.ui.journal.editor.JournalEditorViewModel
import com.example.sweetbean.ui.journal.list.JournalListScreen
import com.example.sweetbean.ui.journal.list.JournalListViewModel
import com.example.sweetbean.ui.journal.tree.EmotionTreeScreen
import com.example.sweetbean.ui.journal.tree.EmotionTreeViewModel

@Composable
fun JournalRoute() {
    val context = LocalContext.current
    val uid = remember { AuthDependencies.firebaseAuth.currentUser?.uid } ?: return

    val listViewModel: JournalListViewModel = viewModel(
        key = "journal_list_$uid",
        factory = remember(uid) { JournalDependencies.listViewModelFactory(uid) }
    )
    val editorViewModel: JournalEditorViewModel = viewModel(
        key = "journal_editor_$uid",
        factory = remember(uid) { JournalDependencies.editorViewModelFactory(uid) }
    )
    val treeViewModel: EmotionTreeViewModel = viewModel(
        key = "journal_tree_$uid",
        factory = remember(uid) { JournalDependencies.emotionTreeViewModelFactory(uid) }
    )

    val listState by listViewModel.uiState.collectAsStateWithLifecycle()
    val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
    val treeState by treeViewModel.uiState.collectAsStateWithLifecycle()

    val (viewMode, setViewMode) = rememberJournalViewMode()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showTree by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    // Lưu thành công thì đóng màn hình soạn; danh sách và lịch tự cập nhật nhờ snapshot listener.
    LaunchedEffect(editorState.isSaved) {
        if (editorState.isSaved) showEditor = false
    }

    // Không kiểm tra được nhật ký của ngày đã chọn thì đóng form (tránh tạo trùng khi mất mạng).
    LaunchedEffect(editorState.loadFailed) {
        if (editorState.loadFailed) showEditor = false
    }

    LaunchedEffect(listState.errorMessage) {
        listState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            listViewModel.consumeError()
        }
    }

    LaunchedEffect(editorState.errorMessage) {
        editorState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            editorViewModel.consumeError()
        }
    }

    LaunchedEffect(treeState.errorMessage) {
        treeState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            treeViewModel.consumeError()
        }
    }

    when {
        showTree -> {
            BackHandler { showTree = false }
            LaunchedEffect(Unit) { treeViewModel.refresh() }   // mở màn hình nào cũng tải lại

            EmotionTreeScreen(
                uiState = treeState,
                onBack = { showTree = false },
                onYearChange = treeViewModel::selectYear,
                onMonthSelect = treeViewModel::selectMonth
            )
        }

        showEditor -> {
            BackHandler { showEditor = false }

            JournalEditorScreen(
                uiState = editorState,
                onBack = { showEditor = false },
                onEmotionSelected = editorViewModel::onEmotionSelected,
                onTitleChange = editorViewModel::onTitleChange,
                onTextChange = editorViewModel::onTextChange,
                onAddMedia = editorViewModel::addMedia,
                onRemoveMedia = editorViewModel::removeMedia,
                onSave = editorViewModel::save
            )
        }

        else -> {
            JournalListScreen(
                uiState = listState,
                viewMode = viewMode,
                onViewModeChange = setViewMode,
                onMonthSelected = listViewModel::selectMonth,
                onYearSelected = listViewModel::selectYear,
                onJournalClick = { selectedId = it.id },
                onDayClick = { day ->
                    val existing = listViewModel.run {
                        listState.journalOn(day)
                    }
                    if (existing != null) {
                        selectedId = existing.id
                    } else {
                        // Chưa có nhật ký: mở form tạo cho đúng ngày này (nếu thực ra đã có, form tự nạp để sửa).
                        editorViewModel.startFor(day)
                        showEditor = true
                    }
                },
                onOpenTree = { showTree = true }
            )

            val selected = remember(listState.groups, selectedId) {
                listState.groups.asSequence()
                    .flatMap { it.items.asSequence() }
                    .firstOrNull { it.id == selectedId }
            }

            if (selected != null) {
                JournalDetailDialog(
                    journal = selected,
                    onDismiss = { selectedId = null },
                    onEdit = {
                        editorViewModel.start(selected)
                        selectedId = null
                        showEditor = true
                    },
                    onDelete = {
                        listViewModel.delete(selected)
                        selectedId = null
                    }
                )
            }
        }
    }
}