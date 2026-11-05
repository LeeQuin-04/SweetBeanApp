package com.example.sweetbean.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.core.UsernameValidator
import com.example.sweetbean.domain.auth.RegisterResult
import com.example.sweetbean.domain.auth.RegisterUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.EmailChanged -> updateForm { it.copy(email = event.value) }

            is RegisterUiEvent.UsernameChanged -> updateForm { it.copy(username = event.value) }

            is RegisterUiEvent.PasswordChanged -> updateForm { it.copy(password = event.value) }

            is RegisterUiEvent.ConfirmPasswordChanged ->
                updateForm { it.copy(confirmPassword = event.value) }

            RegisterUiEvent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            RegisterUiEvent.ToggleConfirmPasswordVisibility -> {
                _uiState.update {
                    it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
                }
            }

            RegisterUiEvent.RegisterClicked -> register()

            RegisterUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun updateForm(transform: (RegisterUiState) -> RegisterUiState) {
        _uiState.update {
            transform(it).copy(
                errorMessage = null,
                isRegistrationSuccessful = false
            )
        }
    }

    private fun validateForm(state: RegisterUiState): String? = when {
        state.email.isBlank() -> AuthMessages.ENTER_EMAIL

        !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() ->
            AuthMessages.EMAIL_INVALID

        state.username.isBlank() -> AuthMessages.ENTER_USERNAME

        !UsernameValidator.isValid(state.username) -> AuthMessages.USERNAME_INVALID

        state.password.length < 6 -> AuthMessages.PASSWORD_TOO_SHORT

        state.confirmPassword.isBlank() -> AuthMessages.ENTER_CONFIRM_PASSWORD

        state.password != state.confirmPassword -> AuthMessages.PASSWORD_MISMATCH

        else -> null
    }

    private fun register() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val validationError = validateForm(currentState)
        if (validationError != null) {
            showError(validationError)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isRegistrationSuccessful = false
                )
            }

            try {
                when (
                    val result = registerUseCase(
                        email = currentState.email,
                        username = currentState.username,
                        password = currentState.password
                    )
                ) {
                    RegisterResult.Success -> accountCreated(message = null)

                    is RegisterResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }

                    // Tài khoản đã tồn tại trên Firebase nên coi như đăng ký xong;
                    // gate sẽ đưa người dùng tới bước còn thiếu (xác minh email / Username).
                    is RegisterResult.AccountCreatedIncomplete ->
                        accountCreated(message = result.message)

                    is RegisterResult.VerificationEmailFailed ->
                        accountCreated(message = result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AuthMessages.REGISTER_FAILED)
                }
            }
        }
    }

    /** Đưa form đăng ký về trạng thái rỗng (gọi khi rời màn hình đăng ký). */
    fun resetState() {
        _uiState.value = RegisterUiState()
    }

    private fun accountCreated(message: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRegistrationSuccessful = true,
                errorMessage = message,
                password = "",
                confirmPassword = ""
            )
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(errorMessage = message, isRegistrationSuccessful = false)
        }
    }
}
