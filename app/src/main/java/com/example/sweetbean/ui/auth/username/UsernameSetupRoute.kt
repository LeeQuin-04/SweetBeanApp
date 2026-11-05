package com.example.sweetbean.ui.auth.username

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UsernameSetupRoute(
    viewModel: UsernameSetupViewModel,
    onUsernameSaved: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.resetState()
            onUsernameSaved()
        }
    }

    UsernameSetupDialog(
        uiState = uiState,
        onUsernameSubmit = viewModel::submitUsername,
        onLogout = {
            viewModel.resetState()
            onLogout()
        }
    )
}
