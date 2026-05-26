package com.tattoo.studio.presentation.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.dto.ConsultationApplicationDto
import com.tattoo.studio.data.remote.dto.MasterApplicationDto
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MasterApplicationsViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _applications = MutableStateFlow<List<MasterApplicationDto>>(emptyList())
    val applications: StateFlow<List<MasterApplicationDto>> = _applications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    private val _actionError = MutableStateFlow<AppError?>(null)
    val actionError: StateFlow<AppError?> = _actionError.asStateFlow()

    private val _consultations = MutableStateFlow<List<ConsultationApplicationDto>>(emptyList())
    val consultations: StateFlow<List<ConsultationApplicationDto>> = _consultations.asStateFlow()

    private val _consultationsLoading = MutableStateFlow(false)
    val consultationsLoading: StateFlow<Boolean> = _consultationsLoading.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getMasterApplications()
            if (result.isSuccess) {
                _applications.value = result.getOrNull() ?: emptyList()
            } else {
                val t = result.exceptionOrNull()
                _error.value = if (t is AppError) t else t?.toAppError() ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun approve(appId: Int) {
        viewModelScope.launch {
            _actionError.value = null
            val result = repository.approveMasterApplication(appId)
            if (result.isSuccess) {
                _applications.value = _applications.value.filter { it.id != appId }
            } else {
                val t = result.exceptionOrNull()
                _actionError.value = if (t is AppError) t else t?.toAppError() ?: AppError.Unknown()
            }
        }
    }

    fun reject(appId: Int) {
        viewModelScope.launch {
            _actionError.value = null
            val result = repository.rejectMasterApplication(appId)
            if (result.isSuccess) {
                _applications.value = _applications.value.filter { it.id != appId }
            } else {
                val t = result.exceptionOrNull()
                _actionError.value = if (t is AppError) t else t?.toAppError() ?: AppError.Unknown()
            }
        }
    }

    fun loadConsultations() {
        viewModelScope.launch {
            _consultationsLoading.value = true
            val result = repository.getConsultationApplications()
            if (result.isSuccess) {
                _consultations.value = result.getOrNull() ?: emptyList()
            }
            _consultationsLoading.value = false
        }
    }

    fun closeConsultation(appId: Int) {
        viewModelScope.launch {
            val result = repository.closeConsultationApplication(appId)
            if (result.isSuccess) {
                _consultations.value = _consultations.value.map {
                    if (it.id == appId) it.copy(status = "completed") else it
                }
            } else {
                val t = result.exceptionOrNull()
                _actionError.value = if (t is AppError) t else t?.toAppError() ?: AppError.Unknown()
            }
        }
    }

    fun clearActionError() { _actionError.value = null }
}
