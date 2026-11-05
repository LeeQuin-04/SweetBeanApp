package com.example.sweetbean.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sweetbean.data.auth.AuthRepository
import com.example.sweetbean.data.auth.FirebaseAuthRepository
import com.example.sweetbean.data.auth.FirebaseGoogleAuthRepository
import com.example.sweetbean.data.auth.GoogleAuthRepository
import com.example.sweetbean.data.user.UserProfileRepository
import com.example.sweetbean.domain.auth.RegisterUseCase
import com.example.sweetbean.ui.auth.forgotpassword.ForgotPasswordViewModel
import com.example.sweetbean.ui.auth.gate.AuthGateViewModel
import com.example.sweetbean.ui.auth.google.GoogleSignInManager
import com.example.sweetbean.ui.auth.google.GoogleSignInViewModel
import com.example.sweetbean.ui.auth.login.LoginViewModel
import com.example.sweetbean.ui.auth.register.RegisterViewModel
import com.example.sweetbean.ui.auth.username.UsernameSetupViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AuthDependencies {

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(firestore)
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(firebaseAuth)
    }

    val googleAuthRepository: GoogleAuthRepository by lazy {
        FirebaseGoogleAuthRepository(
            firebaseAuth = firebaseAuth,
            userProfileRepository = userProfileRepository
        )
    }

    val registerUseCase: RegisterUseCase by lazy {
        RegisterUseCase(
            authRepository = authRepository,
            userProfileRepository = userProfileRepository
        )
    }

    // ViewModelFactory dựng bằng DSL initializer: không cần một lớp Factory riêng cho mỗi ViewModel.

    val loginViewModelFactory: ViewModelProvider.Factory by lazy {
        viewModelFactory {
            initializer { LoginViewModel(authRepository) }
        }
    }

    val authGateViewModelFactory: ViewModelProvider.Factory by lazy {
        viewModelFactory {
            initializer {
                AuthGateViewModel(
                    authRepository = authRepository,
                    firebaseAuth = firebaseAuth,
                    userProfileRepository = userProfileRepository
                )
            }
        }
    }

    val registerViewModelFactory: ViewModelProvider.Factory by lazy {
        viewModelFactory {
            initializer { RegisterViewModel(registerUseCase) }
        }
    }

    val forgotPasswordViewModelFactory: ViewModelProvider.Factory by lazy {
        viewModelFactory {
            initializer { ForgotPasswordViewModel(authRepository) }
        }
    }

    val usernameSetupViewModelFactory: ViewModelProvider.Factory by lazy {
        viewModelFactory {
            initializer {
                UsernameSetupViewModel(
                    userProfileRepository = userProfileRepository,
                    firebaseAuth = firebaseAuth
                )
            }
        }
    }

    fun provideGoogleSignInViewModelFactory(
        googleSignInManager: GoogleSignInManager
    ): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                GoogleSignInViewModel(
                    googleAuthRepository = googleAuthRepository,
                    googleSignInManager = googleSignInManager
                )
            }
        }
    }
}
