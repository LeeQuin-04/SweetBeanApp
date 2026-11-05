package com.example.sweetbean.data.auth

import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.core.UsernameValidator
import com.example.sweetbean.data.user.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseGoogleAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val userProfileRepository: UserProfileRepository
) : GoogleAuthRepository {

    override suspend fun signInWithGoogle(
        idToken: String
    ): GoogleSignInResult {
        if (idToken.isBlank()) {
            return GoogleSignInResult.Error(AuthMessages.GOOGLE_NO_TOKEN)
        }

        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val user = firebaseAuth
                .signInWithCredential(credential)
                .await()
                .user
                ?: return GoogleSignInResult.Error(AuthMessages.GOOGLE_ACCOUNT_NOT_FOUND)

            // Tạo hồ sơ nếu là lần đầu; nếu đã có thì không ghi đè tên hiển thị người dùng đã sửa.
            userProfileRepository.ensureProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName.orEmpty()
            )

            val username = userProfileRepository
                .getUserProfile(user.uid)
                ?.username
                .orEmpty()

            if (UsernameValidator.isValid(username)) {
                GoogleSignInResult.Success
            } else {
                GoogleSignInResult.NeedsUsername
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GoogleSignInResult.Error(AuthMessages.GOOGLE_SIGN_IN_FAILED)
        }
    }
}