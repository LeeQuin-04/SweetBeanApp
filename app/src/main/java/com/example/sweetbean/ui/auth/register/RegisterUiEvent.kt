package com.example.sweetbean.ui.auth.register

sealed interface RegisterUiEvent {

    data class EmailChanged(val value: String) : RegisterUiEvent

    data class UsernameChanged(val value: String) : RegisterUiEvent

    data class PasswordChanged(val value: String) : RegisterUiEvent

    data class ConfirmPasswordChanged(val value: String) : RegisterUiEvent

    data object TogglePasswordVisibility : RegisterUiEvent

    data object ToggleConfirmPasswordVisibility : RegisterUiEvent

    data object RegisterClicked : RegisterUiEvent

    data object ErrorDismissed : RegisterUiEvent
}
