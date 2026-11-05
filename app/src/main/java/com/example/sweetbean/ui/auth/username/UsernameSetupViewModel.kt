package com.example.sweetbean.ui.auth.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetbean.core.AuthMessages
import com.example.sweetbean.core.UsernameValidator
import com.example.sweetbean.data.user.SetUsernameResult
import com.example.sweetbean.data.user.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UsernameSetupUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

class UsernameSetupViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsernameSetupUiState())
    val uiState: StateFlow<UsernameSetupUiState> = _uiState.asStateFlow()

    fun submitUsername(username: String) {
        if (_uiState.value.isLoading) return

        val cleanUsername = UsernameValidator.clean(username)

        if (!UsernameValidator.isValid(cleanUsername)) {
            showError(AuthMessages.USERNAME_INVALID)
            return
        }

        val user = firebaseAuth.currentUser
        if (user == null) {
            showError(AuthMessages.SESSION_EXPIRED)
            return
        }

        viewModelScope.launch {
            _uiState.value = UsernameSetupUiState(isLoading = true)

            try {
                _uiState.value = when (
                    userProfileRepository.setUsername(
                        uid = user.uid,
                        username = cleanUsername
                    )
                ) {
                    SetUsernameResult.Success ->
                        UsernameSetupUiState(isSaved = true)

                    SetUsernameResult.AlreadyTaken ->
                        UsernameSetupUiState(errorMessage = AuthMessages.SETUP_USERNAME_TAKEN)

                    SetUsernameResult.ProfileNotFound ->
                        UsernameSetupUiState(errorMessage = AuthMessages.PROFILE_NOT_FOUND)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(AuthMessages.USERNAME_SAVE_FAILED)
            }
        }
    }

    fun resetState() {
        _uiState.value = UsernameSetupUiState()
    }

    private fun showError(message: String) {
        _uiState.value = UsernameSetupUiState(errorMessage = message)
    }
}
