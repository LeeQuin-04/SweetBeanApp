package com.example.sweetbean.domain.auth

import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.core.UsernameValidator
import com.example.sweetbean.data.auth.AuthRepository
import com.example.sweetbean.data.auth.AuthResult
import com.example.sweetbean.data.auth.CreateAccountResult
import com.example.sweetbean.data.user.SetUsernameResult
import com.example.sweetbean.data.user.UserProfileRepository
import kotlinx.coroutines.CancellationException

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) {

    suspend operator fun invoke(
        email: String,
        username: String,
        password: String
    ): RegisterResult {
        val cleanEmail = email.trim()
        val cleanUsername = UsernameValidator.clean(username)

        if (cleanEmail.isBlank() || cleanUsername.isBlank() || password.isBlank()) {
            return RegisterResult.Error(AuthMessages.ENTER_ALL_REGISTER_FIELDS)
        }

        if (!UsernameValidator.isValid(cleanUsername)) {
            return RegisterResult.Error(AuthMessages.USERNAME_INVALID)
        }

        // Bước 1: Kiểm tra Username trước khi tạo tài khoản.
        try {
            if (!userProfileRepository.isUsernameAvailable(cleanUsername)) {
                return RegisterResult.Error(AuthMessages.USERNAME_TAKEN)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return RegisterResult.Error(AuthMessages.USERNAME_CHECK_FAILED)
        }

        // Bước 2: Tạo tài khoản Firebase.
        val account = when (
            val created = authRepository.createAccount(cleanEmail, password)
        ) {
            is CreateAccountResult.Success -> created.account
            CreateAccountResult.EmailAlreadyInUse ->
                return RegisterResult.Error(AuthMessages.EMAIL_ALREADY_IN_USE)
            CreateAccountResult.Failure ->
                return RegisterResult.Error(AuthMessages.CREATE_ACCOUNT_FAILED)
        }

        // Bước 3: Tạo hồ sơ để setUsername() tìm thấy users/{uid}.
        // Dùng username làm tên hiển thị.
        try {
            userProfileRepository.ensureProfile(
                uid = account.uid,
                email = account.email,
                displayName = cleanUsername
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return RegisterResult.AccountCreatedIncomplete(
                AuthMessages.PROFILE_NOT_SAVED_AFTER_CREATE
            )
        }

        // Bước 4: Lưu Username bằng transaction để chống trùng.
        val usernameResult = try {
            userProfileRepository.setUsername(
                uid = account.uid,
                username = cleanUsername
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return RegisterResult.AccountCreatedIncomplete(
                AuthMessages.USERNAME_NOT_SAVED_AFTER_CREATE
            )
        }

        when (usernameResult) {
            SetUsernameResult.Success -> Unit

            SetUsernameResult.AlreadyTaken ->
                return RegisterResult.AccountCreatedIncomplete(
                    AuthMessages.USERNAME_TAKEN_AFTER_CREATE
                )

            SetUsernameResult.ProfileNotFound ->
                return RegisterResult.AccountCreatedIncomplete(
                    AuthMessages.PROFILE_INCOMPLETE_AFTER_CREATE
                )
        }

        // Bước 5: Gửi email xác minh.
        return when (authRepository.sendEmailVerification()) {
            AuthResult.Success -> RegisterResult.Success
            is AuthResult.Error -> RegisterResult.VerificationEmailFailed(
                AuthMessages.VERIFICATION_EMAIL_FAILED_AFTER_CREATE
            )
        }
    }
}