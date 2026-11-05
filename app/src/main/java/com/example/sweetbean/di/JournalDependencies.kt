package com.example.sweetbean.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sweetbean.data.journal.JournalRepository
import com.example.sweetbean.data.media.CloudinaryMediaUploader
import com.example.sweetbean.ui.journal.editor.JournalEditorViewModel
import com.example.sweetbean.ui.journal.list.JournalListViewModel

object JournalDependencies {

    private val journalRepository: JournalRepository by lazy {
        JournalRepository(AuthDependencies.firestore)
    }

    // Factory nhận uid vì ViewModel sống theo Activity: nếu đăng xuất rồi đăng nhập
    // tài khoản khác thì phải là một ViewModel mới (Route đặt key theo uid).
    fun listViewModelFactory(uid: String): ViewModelProvider.Factory = viewModelFactory {
        initializer { JournalListViewModel(uid, journalRepository) }
    }

    fun editorViewModelFactory(uid: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = checkNotNull(this[APPLICATION_KEY])
            JournalEditorViewModel(
                uid = uid,
                repository = journalRepository,
                uploader = CloudinaryMediaUploader(application.contentResolver)
            )
        }
    }
}