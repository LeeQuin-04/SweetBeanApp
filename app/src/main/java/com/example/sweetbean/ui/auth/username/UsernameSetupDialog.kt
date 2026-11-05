package com.example.sweetbean.ui.auth.username

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import com.example.sweetbean.core.UsernameValidator
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.example.sweetbean.ui.auth.common.DialogShape
import com.example.sweetbean.ui.auth.common.dialogBackgroundImage
import com.example.sweetbean.ui.auth.common.AuthDialogTheme

@Composable
fun UsernameSetupDialog(
    uiState: UsernameSetupUiState,
    onUsernameSubmit: (String) -> Unit,
    onLogout: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    AuthDialogTheme {
        AlertDialog(
            // Không cho chạm ra ngoài hoặc bấm Back để thoát: tránh vô tình bị đăng xuất.
            onDismissRequest = {},
            modifier = Modifier.dialogBackgroundImage(),
            shape = DialogShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,

            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.auth_username_description))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { value ->
                            username = UsernameValidator.sanitizeInput(value)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_username_label)) },
                        placeholder = { Text(stringResource(R.string.auth_username_placeholder)) },
                        supportingText = { Text(stringResource(R.string.auth_username_support)) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password
                        ),
                        isError = uiState.errorMessage != null
                    )

                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (uiState.isLoading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onUsernameSubmit(username) },
                    enabled = !uiState.isLoading && UsernameValidator.isValid(username)
                ) {
                    Text(
                        stringResource(
                            if (uiState.isLoading) {
                                R.string.auth_username_saving
                            } else {
                                R.string.auth_username_confirm
                            }
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onLogout,
                    enabled = !uiState.isLoading
                ) {
                    Text(stringResource(R.string.auth_username_switch_account))
                }
            }
        )
    }
}
