package com.tattoo.studio.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.AuthRepository
import com.tattoo.studio.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Initial)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            val result = userRepository.getMe()
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _uiState.value = EditProfileUiState.Success(
                    fullName = user.fullName,
                    email = user.email,
                    description = user.description ?: ""
                )
            } else {
                _uiState.value = EditProfileUiState.Error(
                    result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
                )
            }
        }
    }

    fun saveProfile(
        fullName: String, 
        email: String, 
        description: String, 
        onSuccess: () -> Unit,
        onNavigateToVerification: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val currentState = uiState.value as? EditProfileUiState.Success
            val emailChanged = currentState?.email != email

            val result = userRepository.updateProfile(
                fullName = fullName,
                email = email,
                description = description
            )
            if (result.isSuccess) {
                if (emailChanged) {
                    onNavigateToVerification(email)
                } else {
                    onSuccess()
                }
            } else {
                val error = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
                _uiState.value = (uiState.value as? EditProfileUiState.Success)?.copy(
                    error = error.userMessage
                ) ?: EditProfileUiState.Error(error)
            }
            _isSaving.value = false
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = userRepository.deleteAccount()
            if (result.isSuccess) {
                authRepository.logout()
                onSuccess()
            } else {
                val error = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
                _uiState.value = (uiState.value as? EditProfileUiState.Success)?.copy(
                    error = error.userMessage
                ) ?: EditProfileUiState.Error(error)
            }
            _isSaving.value = false
        }
    }
}

sealed class EditProfileUiState {
    object Initial : EditProfileUiState()
    object Loading : EditProfileUiState()
    data class Success(
        val fullName: String,
        val email: String,
        val description: String,
        val error: String? = null
    ) : EditProfileUiState()
    data class Error(val error: AppError) : EditProfileUiState()
}
