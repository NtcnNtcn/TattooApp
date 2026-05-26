package com.tattoo.studio.presentation.masters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.api.UsersApi
import com.tattoo.studio.data.remote.api.WorksApi
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.data.remote.safeApiCall
import com.tattoo.studio.data.remote.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MasterDetailViewModel @Inject constructor(
    private val usersApi: UsersApi,
    private val worksApi: WorksApi
) : ViewModel() {

    private val _master = MutableStateFlow<UserDto?>(null)
    val master: StateFlow<UserDto?> = _master.asStateFlow()

    private val _works = MutableStateFlow<List<TattooWorkFeedDto>>(emptyList())
    val works: StateFlow<List<TattooWorkFeedDto>> = _works.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    fun loadMasterData(masterId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // 1. Load Profile
            val profileResult = safeApiCall { usersApi.getUser(masterId) }
            if (profileResult.isSuccess) {
                _master.value = profileResult.getOrNull()
            } else {
                _error.value = profileResult.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
                _isLoading.value = false
                return@launch
            }

            // 2. Load Works
            val worksResult = safeApiCall { worksApi.listWorks(masterId = masterId, size = 100) }
            if (worksResult.isSuccess) {
                _works.value = worksResult.getOrNull()?.items ?: emptyList()
            } else {
                // Not a fatal error if works fail to load, just empty list
                _works.value = emptyList()
            }

            // 3. Load Current User (to check if admin)
            val meResult = safeApiCall { usersApi.getMe() }
            if (meResult.isSuccess) {
                _currentUser.value = meResult.getOrNull()
            }
            
            _isLoading.value = false
        }
    }

    fun updateMasterStatus(masterId: Int, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = safeApiCall { usersApi.updateUserStatus(masterId, newStatus) }
            if (result.isSuccess) {
                _master.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }
}
