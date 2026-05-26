package com.tattoo.studio.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.api.UsersApi
import com.tattoo.studio.data.remote.dto.AdminStatsDto
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.safeApiCall
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.AdminRepository
import com.tattoo.studio.data.repository.AuthRepository
import com.tattoo.studio.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.tattoo.studio.data.local.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    val userProfile: StateFlow<UserDto?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminStatsDto?>(null)
    val adminStats: StateFlow<AdminStatsDto?> = _adminStats.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = userRepository.getMe()
            if (result.isSuccess) {
                val user = result.getOrNull()
                _userProfile.value = user
                if (user?.role?.name?.lowercase() == "admin") {
                    val statsResult = adminRepository.getStats()
                    if (statsResult.isSuccess) {
                        _adminStats.value = statsResult.getOrNull()
                    }
                }
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun uploadAvatar(imageFile: java.io.File, mimeType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = userRepository.uploadAvatar(imageFile, mimeType)
            if (result.isSuccess) {
                _userProfile.value = result.getOrNull()
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
