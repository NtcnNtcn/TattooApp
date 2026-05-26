package com.tattoo.studio.presentation.upload

import androidx.lifecycle.ViewModel
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.WorksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: WorksRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    suspend fun uploadWork(imageFile: File, description: String, tags: String, mimeType: String): Boolean {
        if (tags.isBlank()) {
            _error.value = AppError.Validation(userMessage = "Теги не могут быть пустыми")
            return false
        }
        
        _isLoading.value = true
        _error.value = null
        val result = repository.uploadWork(imageFile, description.takeIf { it.isNotBlank() }, tags, mimeType)
        _isLoading.value = false
        
        return if (result.isSuccess) {
            true
        } else {
            val throwable = result.exceptionOrNull()
            _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                ?: AppError.Unknown()
            false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
