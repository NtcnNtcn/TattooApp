package com.tattoo.studio.presentation.masters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.api.UsersApi
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.safeApiCall
import com.tattoo.studio.data.remote.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MastersListViewModel @Inject constructor(
    private val usersApi: UsersApi
) : ViewModel() {

    private val _masters = MutableStateFlow<List<UserDto>>(emptyList())
    val masters: StateFlow<List<UserDto>> = _masters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    fun loadMasters() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = safeApiCall { usersApi.listUsers(role = "master").items }
            if (result.isSuccess) {
                _masters.value = result.getOrDefault(emptyList())
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }
}
