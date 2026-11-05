package com.example.sweetbean.ui.auth.google

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.data.auth.GoogleAuthRepository
import com.example.sweetbean.data.auth.GoogleSignInResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoogleSignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignInSuccessful: Boolean = false,
    val needsUsername: Boolean = false,
    val wasCancelled: Boolean = false
)

class GoogleSignInViewModel(
    private val googleAuthRepository: GoogleAuthRepository,
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleSignInUiState())
    val uiState: StateFlow<GoogleSignInUiState> = _uiState.asStateFlow()

    fun launchGoogleSignIn(activity: Activity) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = GoogleSignInUiState(isLoading = true)

            try {
                val idToken = googleSignInManager.signIn(activity)
                authenticateWithGoogle(idToken)
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = GoogleSignInUiState(wasCancelled = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = GoogleSignInUiState(
                    errorMessage = AuthMessages.GOOGLE_LAUNCH_FAILED
                )
            }
        }
    }

    private suspend fun authenticateWithGoogle(idToken: String) {
        try {
            _uiState.value = when (
                val result = googleAuthRepository.signInWithGoogle(idToken)
            ) {
                GoogleSignInResult.Success ->
                    GoogleSignInUiState(isSignInSuccessful = true)

                GoogleSignInResult.NeedsUsername ->
                    GoogleSignInUiState(needsUsername = true)

                is GoogleSignInResult.Error ->
                    GoogleSignInUiState(errorMessage = result.message)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = GoogleSignInUiState(
                errorMessage = AuthMessages.GOOGLE_UNEXPECTED_ERROR
            )
        }
    }

    fun resetState() {
        _uiState.value = GoogleSignInUiState()
    }
}
