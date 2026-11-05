package com.example.sweetbean.ui.auth.login

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

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> {
                _uiState.update {
                    it.copy(
                        email = event.value,
                        errorMessage = null,
                        isLoginSuccessful = false
                    )
                }
            }

            is LoginUiEvent.PasswordChanged -> {
                _uiState.update {
                    it.copy(
                        password = event.value,
                        errorMessage = null,
                        isLoginSuccessful = false
                    )
                }
            }

            LoginUiEvent.TogglePasswordVisibility -> {
                _uiState.update {
                    it.copy(isPasswordVisible = !it.isPasswordVisible)
                }
            }

            LoginUiEvent.LoginClicked -> login()

            LoginUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun login() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        when {
            currentState.email.isBlank() -> {
                showError(AuthMessages.ENTER_EMAIL)
                return
            }

            currentState.password.isBlank() -> {
                showError(AuthMessages.ENTER_PASSWORD)
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isLoginSuccessful = false
                )
            }

            try {
                when (
                    val result = authRepository.login(
                        email = currentState.email.trim(),
                        password = currentState.password
                    )
                ) {
                    AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccessful = true,
                                password = ""
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
                    it.copy(isLoading = false, errorMessage = AuthMessages.GENERIC_ERROR)
                }
            }
        }
    }

    /** Đưa form về trạng thái rỗng (gọi khi đăng xuất để không còn email, lỗi cũ). */
    fun resetState() {
        _uiState.value = LoginUiState()
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}
