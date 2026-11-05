package com.example.sweetbean.ui.auth.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.core.UsernameValidator
import com.example.sweetbean.data.auth.AuthRepository
import com.example.sweetbean.data.auth.AuthResult
import com.example.sweetbean.data.user.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class AuthGateViewModel(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private var profileCheckJob: Job? = null
    private var resendCooldownJob: Job? = null

    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds: StateFlow<Int> = _resendCooldownSeconds.asStateFlow()

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 300
    }

    private val _uiState = MutableStateFlow(AuthGateUiState(isInitializing = true))
    val uiState: StateFlow<AuthGateUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        checkAuthState(auth.currentUser)
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    private fun checkAuthState(user: FirebaseUser?) {
        if (user == null) {
            cancelProfileCheck()
            resendCooldownJob?.cancel()
            _resendCooldownSeconds.value = 0
            _uiState.value = AuthGateUiState(isCheckingAuth = false, isSignedOut = true)
            return
        }
        val isGoogleAccount = user.providerData.any { it.providerId == "google.com" }

        if (!isGoogleAccount && !user.isEmailVerified) {
            cancelProfileCheck()
            _uiState.value = AuthGateUiState(
                isCheckingAuth = false,
                isEmailVerificationRequired = true,
                email = user.email
            )
            return
        }

        _uiState.update {
            it.copy(
                isSignedOut = false,
                isCheckingAuth = true,
                isLoggedIn = false,
                isEmailVerificationRequired = false,
                isUsernameSetupRequired = false,
                email = user.email,
                errorMessage = null
            )
        }

        profileCheckJob?.cancel()
        profileCheckJob = viewModelScope.launch {
            try {
                var profile = userProfileRepository.getUserProfile(user.uid)

                if (firebaseAuth.currentUser?.uid != user.uid) return@launch

                // Tài khoản Firebase còn nhưng hồ sơ Firestore bị thiếu:
                // thử tạo lại hồ sơ trước khi yêu cầu thiết lập Username.
                if (profile == null) {
                    userProfileRepository.ensureProfile(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName.orEmpty()
                    )

                    if (firebaseAuth.currentUser?.uid != user.uid) return@launch

                    profile = userProfileRepository.getUserProfile(user.uid)
                }

                _uiState.value = when {
                    profile == null -> AuthGateUiState(
                        isCheckingAuth = false,
                        email = user.email,
                        errorMessage = AuthMessages.PROFILE_RECOVER_FAILED
                    )

                    !UsernameValidator.isValid(profile.username) -> AuthGateUiState(
                        isCheckingAuth = false,
                        isUsernameSetupRequired = true,
                        email = user.email
                    )

                    else -> AuthGateUiState(
                        isCheckingAuth = false,
                        isLoggedIn = true,
                        email = user.email
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AuthGateUiState(
                    isCheckingAuth = false,
                    email = user.email,
                    errorMessage = AuthMessages.PROFILE_CHECK_FAILED
                )
            }
        }
    }

    private fun cancelProfileCheck() {
        profileCheckJob?.cancel()
        profileCheckJob = null
    }

    fun refreshVerificationStatus() {
        if (_uiState.value.isCheckingAuth) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingAuth = true, errorMessage = null) }

            try {
                when (val result = authRepository.reloadCurrentUser()) {
                    AuthResult.Success -> {
                        checkAuthState(firebaseAuth.currentUser)
                        if (_uiState.value.isEmailVerificationRequired) {
                            _uiState.update { it.copy(errorMessage = AuthMessages.EMAIL_NOT_VERIFIED) }
                        }
                    }

                    is AuthResult.Error -> _uiState.update {
                        it.copy(isCheckingAuth = false, errorMessage = result.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingAuth = false,
                        errorMessage = AuthMessages.EMAIL_CHECK_FAILED
                    )
                }
            }
        }
    }

    fun resendVerificationEmail() {
        val currentState = _uiState.value
        if (currentState.isCheckingAuth || currentState.isSendingVerificationEmail) return
        if (_resendCooldownSeconds.value > 0) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSendingVerificationEmail = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            try {
                when (val result = authRepository.sendEmailVerification()) {
                    AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isSendingVerificationEmail = false,
                                infoMessage = AuthMessages.VERIFICATION_RESENT
                            )
                        }
                    }

                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSendingVerificationEmail = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingVerificationEmail = false,
                        errorMessage = AuthMessages.VERIFICATION_SEND_FAILED
                    )
                }
            }

            syncResendCooldown()
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = AuthMessages.LOGOUT_FAILED)
                }
            }
        }
    }

    private fun startResendCooldown(seconds: Int) {
        resendCooldownJob?.cancel()
        resendCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _resendCooldownSeconds.value = remaining
                delay(1000.milliseconds)
            }
            _resendCooldownSeconds.value = 0
        }
    }

    fun syncResendCooldown() {
        val elapsed = authRepository.secondsSinceLastVerificationRequest()
        val remaining = if (elapsed == null) 0 else (RESEND_COOLDOWN_SECONDS - elapsed).toInt()

        if (remaining > 0) {
            startResendCooldown(remaining)
        } else {
            resendCooldownJob?.cancel()
            _resendCooldownSeconds.value = 0
        }
    }

    fun retryProfileCheck() {
        checkAuthState(firebaseAuth.currentUser)
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}
