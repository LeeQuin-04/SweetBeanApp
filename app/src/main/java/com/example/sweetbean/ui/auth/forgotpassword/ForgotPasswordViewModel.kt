package com.example.sweetbean.ui.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.data.auth.AuthRepository
import com.example.sweetbean.data.auth.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            is ForgotPasswordUiEvent.EmailChanged -> {
                _uiState.update {
                    it.copy(
                        email = event.value,
                        errorMessage = null,
                        infoMessage = null,
                        isResetEmailSent = false
                    )
                }
            }

            ForgotPasswordUiEvent.SendResetEmailClicked -> sendResetEmail()

            ForgotPasswordUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(errorMessage = null) }
            }

            ForgotPasswordUiEvent.MessageDismissed -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }

    private fun sendResetEmail() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val email = currentState.email.trim()

        if (email.isBlank()) {
            showError(AuthMessages.ENTER_EMAIL)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(AuthMessages.EMAIL_INVALID)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null,
                    isResetEmailSent = false
                )
            }

            try {
                when (val result = authRepository.sendPasswordResetEmail(email)) {
                    AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                infoMessage = AuthMessages.RESET_EMAIL_SENT,
                                isResetEmailSent = true
                            )
                        }
                    }

                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = AuthMessages.RESET_REQUEST_FAILED
                    )
                }
            }
        }
    }

    /** Đưa dialog về trạng thái rỗng (gọi khi đóng dialog). */
    fun resetState() {
        _uiState.value = ForgotPasswordUiState()
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                errorMessage = message,
                infoMessage = null,
                isResetEmailSent = false
            )
        }
    }
}
