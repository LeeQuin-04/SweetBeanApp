package com.example.sweetbean.ui.auth.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import com.example.sweetbean.ui.auth.common.AuthBackground

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLoginClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.auth_register_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = stringResource(R.string.auth_register_subtitle))

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_email_label)) },
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.username,
                onValueChange = onUsernameChange,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_username_label)) },
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_password_label)) },
                singleLine = true,
                enabled = !uiState.isLoading,
                visualTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(
                        onClick = onTogglePasswordVisibility,
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isPasswordVisible) {
                                    R.string.auth_password_hide
                                } else {
                                    R.string.auth_password_show
                                }
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                singleLine = true,
                enabled = !uiState.isLoading,
                visualTransformation = if (uiState.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(
                        onClick = onToggleConfirmPasswordVisibility,
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isConfirmPasswordVisible) {
                                    R.string.auth_password_hide
                                } else {
                                    R.string.auth_password_show
                                }
                            )
                        )
                    }
                }
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.auth_register_button))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBackToLoginClick,
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.auth_register_have_account))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        uiState = RegisterUiState(),
        onEmailChange = {},
        onUsernameChange = {},
        onPasswordChange = {},
        onConfirmPasswordChange = {},
        onTogglePasswordVisibility = {},
        onToggleConfirmPasswordVisibility = {},
        onRegisterClick = {},
        onBackToLoginClick = {}
    )
}

