package com.example.sweetbean.ui.auth.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sweetbean.di.AuthDependencies

@Composable
fun RegisterRoute(
    onBackToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel(
        factory = AuthDependencies.registerViewModelFactory
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegisterScreen(
        uiState = uiState,
        onEmailChange = { viewModel.onEvent(RegisterUiEvent.EmailChanged(it)) },
        onUsernameChange = { viewModel.onEvent(RegisterUiEvent.UsernameChanged(it)) },
        onPasswordChange = { viewModel.onEvent(RegisterUiEvent.PasswordChanged(it)) },
        onConfirmPasswordChange = {
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged(it))
        },
        onTogglePasswordVisibility = {
            viewModel.onEvent(RegisterUiEvent.TogglePasswordVisibility)
        },
        onToggleConfirmPasswordVisibility = {
            viewModel.onEvent(RegisterUiEvent.ToggleConfirmPasswordVisibility)
        },
        onRegisterClick = { viewModel.onEvent(RegisterUiEvent.RegisterClicked) },
        onBackToLoginClick = onBackToLogin
    )

    LaunchedEffect(uiState.isRegistrationSuccessful) {
        if (uiState.isRegistrationSuccessful) {
            onRegistrationSuccess()
        }
    }
}
