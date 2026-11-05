package com.example.sweetbean.ui.auth.google

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

/**
 * Chỉ khởi chạy luồng đăng nhập Google và báo kết quả qua callback.
 */
@Composable
fun GoogleSignInRoute(
    viewModel: GoogleSignInViewModel,
    onSignInSuccessful: () -> Unit,
    onNeedsUsername: () -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        val activity = context.findActivity()

        if (activity != null) {
            viewModel.launchGoogleSignIn(activity)
        }
    }

    val errorMessage = uiState.errorMessage

    LaunchedEffect(
        uiState.isSignInSuccessful,
        uiState.needsUsername,
        uiState.wasCancelled,
        errorMessage
    ) {
        when {
            uiState.isSignInSuccessful -> onSignInSuccessful()
            uiState.needsUsername -> onNeedsUsername()
            uiState.wasCancelled -> onCancelled()
            errorMessage != null -> onError(errorMessage)
        }
    }
}
