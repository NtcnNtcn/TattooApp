package com.tattoo.studio.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.api.ReportsApi
import com.tattoo.studio.data.remote.dto.ReportSummaryDto
import com.tattoo.studio.data.remote.safeApiCall
import com.tattoo.studio.data.remote.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsApi: ReportsApi
) : ViewModel() {

    private val _summary = MutableStateFlow<ReportSummaryDto?>(null)
    val summary: StateFlow<ReportSummaryDto?> = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    fun loadSummary() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = safeApiCall { reportsApi.getSummary() }
            if (result.isSuccess) {
                _summary.value = result.getOrNull()
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun exportReport(format: String) {
        viewModelScope.launch {
            val result = safeApiCall { reportsApi.exportReport(format = format) }
            if (result.isFailure) {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
