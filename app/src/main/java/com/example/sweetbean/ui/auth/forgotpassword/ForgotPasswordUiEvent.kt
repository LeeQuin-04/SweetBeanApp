package com.example.sweetbean.ui.auth.forgotpassword

sealed interface ForgotPasswordUiEvent {

    data class EmailChanged(val value: String) : ForgotPasswordUiEvent

    data object SendResetEmailClicked : ForgotPasswordUiEvent

    data object ErrorDismissed : ForgotPasswordUiEvent

    data object MessageDismissed : ForgotPasswordUiEvent
}
