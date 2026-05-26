package com.tattoo.studio.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.api.SocialApi
import com.tattoo.studio.data.remote.api.WorksApi
import com.tattoo.studio.data.remote.dto.TattooWorkDetailDto
import com.tattoo.studio.data.remote.safeApiCall
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.ApplicationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.tattoo.studio.data.local.NetworkMonitor
import com.tattoo.studio.data.local.prefs.UserPreferences
import com.tattoo.studio.data.repository.SocialRepository
import com.tattoo.studio.data.repository.WorksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkDetailViewModel @Inject constructor(
    private val worksRepository: WorksRepository,
    private val socialRepository: SocialRepository,
    private val applicationsRepository: ApplicationsRepository,
    networkMonitor: NetworkMonitor,
    userPreferences: UserPreferences
) : ViewModel() {

    val userRole: StateFlow<String?> = userPreferences.userRoleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _work = MutableStateFlow<TattooWorkDetailDto?>(null)
    val work: StateFlow<TattooWorkDetailDto?> = _work.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    private val _bookingSuccess = MutableStateFlow<Boolean?>(null)
    val bookingSuccess: StateFlow<Boolean?> = _bookingSuccess.asStateFlow()

    fun loadWork(workId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = worksRepository.getWorkDetail(workId)
            if (result.isSuccess) {
                _work.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun toggleLike(workId: Int) {
        viewModelScope.launch {
            val result = socialRepository.toggleLike(workId)
            if (result.isSuccess) {
                // Refresh work data to get updated like count
                loadWork(workId)
            }
        }
    }

    fun toggleFavorite(workId: Int) {
        viewModelScope.launch {
            val result = socialRepository.toggleFavorite(workId)
            if (result.isSuccess) {
                // Ideally we'd update local state, but refreshing is simpler
                loadWork(workId)
            }
        }
    }

    fun createApplication(masterId: Int, contactInfo: String, message: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _bookingSuccess.value = null
            
            val result = applicationsRepository.createApplication(masterId, contactInfo, message)
            if (result.isSuccess) {
                _bookingSuccess.value = true
            } else {
                _bookingSuccess.value = false
                _error.value = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }
    
    fun resetBookingStatus() {
        _bookingSuccess.value = null
    }
}
