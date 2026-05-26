package com.tattoo.studio.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.local.prefs.UserPreferences
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.api.UsersApi
import com.tattoo.studio.data.remote.safeApiCall
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val usersApi: UsersApi,
    private val userPrefs: UserPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    suspend fun login(email: String, password: String): Boolean {
        _isLoading.value = true
        _error.value = null
        val result = repository.login(email, password)
        return if (result.isSuccess) {
            fetchAndSaveUserInfo()
            _isLoading.value = false
            true
        } else {
            _isLoading.value = false
            val throwable = result.exceptionOrNull()
            _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                ?: AppError.Unknown()
            false
        }
    }

    suspend fun register(email: String, password: String, fullName: String, role: String): Boolean {
        _isLoading.value = true
        _error.value = null
        val result = repository.register(email, password, fullName, role)
        return if (result.isSuccess) {
            _isLoading.value = false
            true
        } else {
            _isLoading.value = false
            val throwable = result.exceptionOrNull()
            _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                ?: AppError.Unknown()
            false
        }
    }

    private suspend fun fetchAndSaveUserInfo() {
        try {
            val user = usersApi.getMe()
            userPrefs.saveUserInfo(user.role.name, user.email)
        } catch (e: Exception) {
            // Best-effort — do not block login flow
        }
    }

    fun clearError() {
        _error.value = null
    }
}
