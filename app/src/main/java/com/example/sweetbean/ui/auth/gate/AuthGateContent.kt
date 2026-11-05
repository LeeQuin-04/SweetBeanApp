package com.example.sweetbean.ui.auth.gate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import com.example.sweetbean.ui.auth.login.LoginRoute
import com.example.sweetbean.ui.auth.login.LoginViewModel
import com.example.sweetbean.ui.splash.SplashScreen

/**
 * Chọn màn hình theo trạng thái của cổng xác thực.
 */
@Composable
fun AuthGateContent(
    uiState: AuthGateUiState,
    loginViewModel: LoginViewModel,
    onGoogleLogin: () -> Unit,
    onRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onRetryProfileCheck: () -> Unit,
    onLogout: () -> Unit
) {
    when {
        uiState.isInitializing -> {
            SplashScreen()
        }

        uiState.isLoggedIn -> {
            LaunchedEffect(Unit) {
                onNavigateToHome()
            }
        }

        uiState.errorMessage != null && !uiState.isEmailVerificationRequired -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = uiState.errorMessage)

                Button(onClick = onRetryProfileCheck) {
                    Text(stringResource(R.string.auth_gate_retry))
                }

                Button(onClick = onLogout) {
                    Text(stringResource(R.string.auth_action_logout))
                }
            }
        }

        else -> {
            LoginRoute(
                viewModel = loginViewModel,
                onGoogleLogin = onGoogleLogin,
                onRegister = onRegister
            )
        }
    }
}