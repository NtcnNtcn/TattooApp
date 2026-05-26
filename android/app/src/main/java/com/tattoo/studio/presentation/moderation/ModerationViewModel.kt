package com.tattoo.studio.presentation.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.dto.TattooWorkOutDto
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val repository: ModerationRepository
) : ViewModel() {

    private val _pendingWorks = MutableStateFlow<List<TattooWorkOutDto>>(emptyList())
    val pendingWorks: StateFlow<List<TattooWorkOutDto>> = _pendingWorks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    /** Transient action error (approve/reject failed) — shown as snackbar */
    private val _actionError = MutableStateFlow<AppError?>(null)
    val actionError: StateFlow<AppError?> = _actionError.asStateFlow()

    fun loadPendingWorks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getPendingWorks()
            if (result.isSuccess) {
                _pendingWorks.value = result.getOrNull() ?: emptyList()
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun approve(id: Int) {
        viewModelScope.launch {
            _actionError.value = null
            val result = repository.approveWork(id)
            if (result.isSuccess) {
                _pendingWorks.value = _pendingWorks.value.filter { it.id != id }
            } else {
                val throwable = result.exceptionOrNull()
                _actionError.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
        }
    }

    fun reject(id: Int, reason: String) {
        viewModelScope.launch {
            _actionError.value = null
            val result = repository.rejectWork(id, reason)
            if (result.isSuccess) {
                _pendingWorks.value = _pendingWorks.value.filter { it.id != id }
            } else {
                val throwable = result.exceptionOrNull()
                _actionError.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearActionError() { _actionError.value = null }
}
