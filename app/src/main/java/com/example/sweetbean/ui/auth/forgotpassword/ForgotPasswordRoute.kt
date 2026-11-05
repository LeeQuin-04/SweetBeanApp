package com.example.sweetbean.ui.auth.forgotpassword

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sweetbean.di.AuthDependencies

@Composable
fun ForgotPasswordRoute(
    onDismiss: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(
        factory = AuthDependencies.forgotPasswordViewModelFactory
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordDialog(
        uiState = uiState,
        onEmailChange = { viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged(it)) },
        onSendResetEmail = { viewModel.onEvent(ForgotPasswordUiEvent.SendResetEmailClicked) },
        onDismiss = {
            viewModel.resetState()
            onDismiss()
        }
    )
}
