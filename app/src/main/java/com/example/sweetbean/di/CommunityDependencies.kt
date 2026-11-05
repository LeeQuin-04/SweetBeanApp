package com.example.sweetbean.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sweetbean.data.community.CommunityApi
import com.example.sweetbean.data.community.CommunityRepository
import com.example.sweetbean.data.media.CloudinaryMediaUploader
import com.example.sweetbean.ui.community.create.CreatePostViewModel
import com.example.sweetbean.ui.community.feed.CommunityFeedViewModel
import com.example.sweetbean.ui.community.detail.PostDetailViewModel
import com.example.sweetbean.ui.community.edit.EditPostViewModel
import com.example.sweetbean.ui.community.search.SearchViewModel
object CommunityDependencies {

    private val repository: CommunityRepository by lazy {
        CommunityRepository(AuthDependencies.firestore)
    }

    private val api: CommunityApi by lazy {
        CommunityApi(AuthDependencies.firebaseAuth)
    }

    fun feedViewModelFactory(uid: String): ViewModelProvider.Factory = viewModelFactory {
        initializer { CommunityFeedViewModel(uid, repository, api) }
    }

    fun createPostViewModelFactory(uid: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = checkNotNull(this[APPLICATION_KEY])
            CreatePostViewModel(
                uid = uid,
                repository = repository,
                api = api,
                uploader = CloudinaryMediaUploader(application.contentResolver)
            )
        }
    }
    fun postDetailViewModelFactory(uid: String, postId: String): ViewModelProvider.Factory =
        viewModelFactory {
            initializer { PostDetailViewModel(uid, postId, repository, api) }
        }
    fun editPostViewModelFactory(uid: String, postId: String): ViewModelProvider.Factory =
        viewModelFactory {
            initializer { EditPostViewModel(uid, postId, repository, api) }
        }
    fun searchViewModelFactory(uid: String): ViewModelProvider.Factory = viewModelFactory {
        initializer { SearchViewModel(uid, repository) }
    }
}