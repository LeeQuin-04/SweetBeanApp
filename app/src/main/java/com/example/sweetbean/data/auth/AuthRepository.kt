package com.example.sweetbean.data.auth

data class CreatedAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

sealed interface CreateAccountResult {
    data class Success(val account: CreatedAccount) : CreateAccountResult
    data object EmailAlreadyInUse : CreateAccountResult
    data object Failure : CreateAccountResult
}

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): AuthResult

    /** Chỉ tạo tài khoản Firebase. Quy trình đăng ký đầy đủ nằm ở RegisterUseCase. */
    suspend fun createAccount(
        email: String,
        password: String
    ): CreateAccountResult

    suspend fun sendPasswordResetEmail(
        email: String
    ): AuthResult

    suspend fun sendEmailVerification(): AuthResult

    suspend fun reloadCurrentUser(): AuthResult

    fun isEmailVerified(): Boolean

    suspend fun logout()
    fun secondsSinceLastVerificationRequest(): Long?
}
