package com.example.sweetbean.ui.auth.gate

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sweetbean.di.AuthDependencies
import com.example.sweetbean.ui.auth.google.GoogleSignInManager
import com.example.sweetbean.ui.auth.google.GoogleSignInRoute
import com.example.sweetbean.ui.auth.google.GoogleSignInViewModel
import com.example.sweetbean.ui.auth.login.LoginViewModel
import com.example.sweetbean.ui.auth.register.RegisterRoute
import com.example.sweetbean.ui.auth.register.RegisterViewModel
import com.example.sweetbean.ui.auth.username.UsernameSetupRoute
import com.example.sweetbean.ui.auth.username.UsernameSetupViewModel
import com.example.sweetbean.ui.auth.verification.EmailVerificationDialog

@Composable
fun AuthGateRoute(
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    val authGateViewModel: AuthGateViewModel = viewModel(
        factory = AuthDependencies.authGateViewModelFactory
    )
    val authUiState by authGateViewModel.uiState.collectAsStateWithLifecycle()

    val loginViewModel: LoginViewModel = viewModel(
        factory = AuthDependencies.loginViewModelFactory
    )
    val registerViewModel: RegisterViewModel = viewModel(
        factory = AuthDependencies.registerViewModelFactory
    )
    val usernameSetupViewModel: UsernameSetupViewModel = viewModel(
        factory = AuthDependencies.usernameSetupViewModelFactory
    )

    val googleSignInManager = remember(context) { GoogleSignInManager(context) }
    val googleSignInViewModel: GoogleSignInViewModel = viewModel(
        factory = AuthDependencies.provideGoogleSignInViewModelFactory(googleSignInManager)
    )

    val googleUiState by googleSignInViewModel.uiState.collectAsStateWithLifecycle()
    val resendCooldownSeconds by authGateViewModel.resendCooldownSeconds.collectAsStateWithLifecycle()

    var showGoogleSignIn by rememberSaveable { mutableStateOf(false) }
    var showRegisterScreen by rememberSaveable { mutableStateOf(false) }
    var wasSignedOut by rememberSaveable { mutableStateOf(false) }
    // Các ViewModel sống theo Activity nên giữ nguyên dữ liệu cũ. Khi đăng xuất, xoá form đăng nhập (email, lỗi cũ) để lần sau quay lại là một form sạch.
    LaunchedEffect(authUiState.isSignedOut) {
        if (authUiState.isSignedOut && !wasSignedOut) {
            loginViewModel.resetState()
        }
        wasSignedOut = authUiState.isSignedOut
    }

    // Hiển thị dialog thẳng từ trạng thái của gate, không giữ thêm một biến trạng thái thứ hai.
    val showUsernameDialog =
        authUiState.isUsernameSetupRequired && !authUiState.isCheckingAuth
    val showVerificationDialog = authUiState.isEmailVerificationRequired && !showRegisterScreen

    // Mỗi khi dialog Xác minh hiện ra (kể cả sau khi màn Đăng ký đóng), tính lại thời gian chờ.
    LaunchedEffect(showVerificationDialog) {
        if (showVerificationDialog) {
            authGateViewModel.syncResendCooldown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showRegisterScreen) {
            RegisterRoute(
                viewModel = registerViewModel,
                onBackToLogin = {
                    registerViewModel.resetState()
                    showRegisterScreen = false
                },
                onRegistrationSuccess = {
                    registerViewModel.resetState()
                    showRegisterScreen = false
                }
            )
        } else {
            AuthGateContent(
                uiState = authUiState,
                loginViewModel = loginViewModel,
                onGoogleLogin = { showGoogleSignIn = true },
                onRegister = { showRegisterScreen = true },
                onNavigateToHome = onNavigateToHome,
                onRetryProfileCheck = authGateViewModel::retryProfileCheck,
                onLogout = authGateViewModel::logout
            )
        }

        if (showGoogleSignIn) {
            GoogleSignInRoute(
                viewModel = googleSignInViewModel,
                onSignInSuccessful = {
                    googleSignInViewModel.resetState()
                    showGoogleSignIn = false
                    authGateViewModel.retryProfileCheck()
                },
                onNeedsUsername = {
                    // Gate tự phát hiện Username còn thiếu và mở dialog.
                    googleSignInViewModel.resetState()
                    showGoogleSignIn = false
                    authGateViewModel.retryProfileCheck()
                },
                onCancelled = {
                    googleSignInViewModel.resetState()
                    showGoogleSignIn = false
                },
                onError = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    googleSignInViewModel.resetState()
                    showGoogleSignIn = false
                }
            )

            if (googleUiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .pointerInput(Unit) { }, // chặn chạm xuống màn Đăng nhập
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showUsernameDialog) {

        UsernameSetupRoute(
            viewModel = usernameSetupViewModel,
            onUsernameSaved = { authGateViewModel.retryProfileCheck() },
            onLogout = {
                googleSignInViewModel.resetState()
                authGateViewModel.logout()
            }
        )
    }
    if (showVerificationDialog) {
        EmailVerificationDialog(
            email = authUiState.email,
            isLoading = authUiState.isCheckingAuth || authUiState.isSendingVerificationEmail,
            resendCooldownSeconds = resendCooldownSeconds,
            errorMessage = authUiState.errorMessage,
            infoMessage = authUiState.infoMessage,
            onCheckVerification = authGateViewModel::refreshVerificationStatus,
            onResendEmail = authGateViewModel::resendVerificationEmail,
            onLogout = authGateViewModel::logout
        )
    }
}
