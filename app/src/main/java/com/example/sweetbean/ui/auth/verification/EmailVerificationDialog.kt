package com.example.sweetbean.ui.auth.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import androidx.compose.ui.graphics.Color
import com.example.sweetbean.ui.auth.common.DialogShape
import com.example.sweetbean.ui.auth.common.dialogBackgroundImage
import com.example.sweetbean.ui.auth.common.AuthDialogTheme

@Composable
fun EmailVerificationDialog(
    email: String?,
    isLoading: Boolean,
    resendCooldownSeconds: Int,
    errorMessage: String?,
    infoMessage: String?,
    onCheckVerification: () -> Unit,
    onResendEmail: () -> Unit,
    onLogout: () -> Unit
) {
    AuthDialogTheme {
        AlertDialog(
            // Không cho chạm ra ngoài hoặc bấm Back để thoát.
            onDismissRequest = { },
            modifier = Modifier.dialogBackgroundImage(),
            shape = DialogShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = stringResource(R.string.auth_verify_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.auth_verify_intro))

                    Text(
                        text = email ?: stringResource(R.string.auth_verify_unknown_email),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(R.string.auth_verify_instructions),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (infoMessage != null) {
                        Text(
                            text = infoMessage,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator()
                    }

                    OutlinedButton(
                        onClick = onResendEmail,
                        enabled = !isLoading && resendCooldownSeconds == 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (resendCooldownSeconds > 0) {
                                    stringResource(
                                        R.string.auth_verify_resend_wait,
                                        "%d:%02d".format(resendCooldownSeconds / 60, resendCooldownSeconds % 60)
                                    )
                                } else {
                                    stringResource(R.string.auth_verify_resend)
                                }
                            )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onCheckVerification,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.auth_verify_done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onLogout,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.auth_verify_switch_email))
                }
            }
        )
    }
}