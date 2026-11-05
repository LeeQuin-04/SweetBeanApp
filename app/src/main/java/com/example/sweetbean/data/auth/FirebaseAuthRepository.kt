package com.example.sweetbean.data.auth

import com.example.sweetbean.core.AuthMessages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.FirebaseTooManyRequestsException
import android.os.SystemClock
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {
    private var lastVerificationRequestUid: String? = null
    private var lastVerificationRequestAt: Long = 0L

    private fun recordVerificationRequest(uid: String) {
        lastVerificationRequestUid = uid
        lastVerificationRequestAt = SystemClock.elapsedRealtime()
    }

    override fun secondsSinceLastVerificationRequest(): Long? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        if (uid != lastVerificationRequestUid) return null

        return (SystemClock.elapsedRealtime() - lastVerificationRequestAt) / 1000
    }

    override suspend fun login(
        email: String,
        password: String
    ): AuthResult {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || password.isBlank()) {
            return AuthResult.Error(AuthMessages.ENTER_EMAIL_AND_PASSWORD)
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return AuthResult.Error(AuthMessages.EMAIL_INVALID)
        }

        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(cleanEmail, password)
                .await()

            val user = result.user
                ?: return AuthResult.Error(AuthMessages.ACCOUNT_NOT_FOUND)

            if (!user.isEmailVerified) {
                return AuthResult.Error(AuthMessages.EMAIL_NOT_VERIFIED)
            }

            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(AuthMessages.LOGIN_FAILED)
        }
    }

    override suspend fun createAccount(
        email: String,
        password: String
    ): CreateAccountResult {
        val cleanEmail = email.trim()

        return try {
            val user = firebaseAuth
                .createUserWithEmailAndPassword(cleanEmail, password)
                .await()
                .user
                ?: return CreateAccountResult.Failure

            CreateAccountResult.Success(
                CreatedAccount(
                    uid = user.uid,
                    email = user.email ?: cleanEmail,
                    displayName = user.displayName.orEmpty(),
                    photoUrl = user.photoUrl?.toString()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthUserCollisionException) {
            CreateAccountResult.EmailAlreadyInUse
        } catch (e: Exception) {
            CreateAccountResult.Failure
        }
    }

    override suspend fun sendEmailVerification(): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error(AuthMessages.NOT_LOGGED_IN)

            user.sendEmailVerification().await()
            recordVerificationRequest(user.uid)
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseTooManyRequestsException) {
            Log.e("AuthRepo", "sendEmailVerification bị giới hạn tốc độ", e)
            // Bị giới hạn cũng tính như vừa gửi để khoá nút thêm 5 phút.
            firebaseAuth.currentUser?.uid?.let { recordVerificationRequest(it) }
            AuthResult.Error(AuthMessages.TOO_MANY_REQUESTS, isRateLimited = true)
        } catch (e: Exception) {
            Log.e("AuthRepo", "sendEmailVerification thất bại", e)
            AuthResult.Error(AuthMessages.VERIFICATION_SEND_FAILED)
        }
    }

    override suspend fun reloadCurrentUser(): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error(AuthMessages.SESSION_NOT_FOUND)

            user.reload().await()
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(AuthMessages.ACCOUNT_REFRESH_FAILED)
        }
    }

    override fun isEmailVerified(): Boolean {
        return firebaseAuth.currentUser?.isEmailVerified == true
    }

    override suspend fun sendPasswordResetEmail(
        email: String
    ): AuthResult {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank()) {
            return AuthResult.Error(AuthMessages.ENTER_EMAIL)
        }

        return try {
            firebaseAuth.sendPasswordResetEmail(cleanEmail).await()
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(AuthMessages.RESET_EMAIL_FAILED)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }
}
