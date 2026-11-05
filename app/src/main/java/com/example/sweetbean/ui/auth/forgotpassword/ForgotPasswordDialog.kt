package com.example.sweetbean.ui.auth.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import androidx.compose.ui.graphics.Color
import com.example.sweetbean.ui.auth.common.DialogShape
import com.example.sweetbean.ui.auth.common.dialogBackgroundImage
import com.example.sweetbean.ui.auth.common.AuthDialogTheme

@Composable
fun ForgotPasswordDialog(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendResetEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    AuthDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.dialogBackgroundImage(),
            shape = DialogShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            title = {
                Text(stringResource(R.string.auth_forgot_title))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.auth_forgot_description))

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_email_label)) },
                        placeholder = { Text(stringResource(R.string.auth_forgot_email_placeholder)) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        )
                    )

                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    }

                    uiState.errorMessage?.let { message ->
                        Text(message)
                    }

                    uiState.infoMessage?.let { message ->
                        Text(message)
                    }

                    if (uiState.isResetEmailSent) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.auth_forgot_sent_hint))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSendResetEmail,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        stringResource(
                            if (uiState.isLoading) {
                                R.string.auth_forgot_sending
                            } else {
                                R.string.auth_forgot_send
                            }
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !uiState.isLoading
                ) {
                    Text(stringResource(R.string.auth_forgot_close))
                }
            }
        )
    }
}
