package com.example.sweetbean.ui.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sweetbean.ui.auth.forgotpassword.ForgotPasswordRoute
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onGoogleLogin: () -> Unit,
    onRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }

    LoginScreen(
        uiState = uiState,
        onEmailChange = { viewModel.onEvent(LoginUiEvent.EmailChanged(it)) },
        onPasswordChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
        onTogglePasswordVisibility = {
            viewModel.onEvent(LoginUiEvent.TogglePasswordVisibility)
        },
        onLoginClick = { viewModel.onEvent(LoginUiEvent.LoginClicked) },
        onGoogleLoginClick = onGoogleLogin,
        onForgotPasswordClick = { showForgotPasswordDialog = true },
        onRegisterClick = onRegister
    )

    if (showForgotPasswordDialog) {
        ForgotPasswordRoute(
            onDismiss = { showForgotPasswordDialog = false }
        )
    }
}
