package com.example.sweetbean.ui.auth.google

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.sweetbean.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInManager(
    context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * Trả về Google ID token. Việc người dùng huỷ (GetCredentialCancellationException)
     * và CancellationException của coroutine được ném nguyên để tầng trên xử lý riêng.
     */
    suspend fun signIn(activity: Activity): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }

        throw IllegalStateException("Loại thông tin đăng nhập Google không hợp lệ.")
    }
}
