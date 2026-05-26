package com.tattoo.studio.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.dto.SubscriptionDto
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository
) : ViewModel() {

    private val _likes = MutableStateFlow<List<TattooWorkFeedDto>>(emptyList())
    val likes: StateFlow<List<TattooWorkFeedDto>> = _likes.asStateFlow()

    private val _favorites = MutableStateFlow<List<TattooWorkFeedDto>>(emptyList())
    val favorites: StateFlow<List<TattooWorkFeedDto>> = _favorites.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SubscriptionDto>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionDto>> = _subscriptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    fun loadSocialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val likesResult = repository.getLikes()
            val favoritesResult = repository.getFavorites()
            
            if (likesResult.isSuccess) {
                _likes.value = likesResult.getOrNull() ?: emptyList()
            }
            
            if (favoritesResult.isSuccess) {
                _favorites.value = favoritesResult.getOrNull() ?: emptyList()
            }
            
            if (likesResult.isFailure || favoritesResult.isFailure) {
                val throwable = likesResult.exceptionOrNull() ?: favoritesResult.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            
            _isLoading.value = false
        }
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getSubscriptions()
            if (result.isSuccess) {
                _subscriptions.value = result.getOrNull() ?: emptyList()
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun toggleFavorite(workId: Int) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(workId)
            if (result.isSuccess) {
                loadSocialData()
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
        }
    }

    fun toggleLike(workId: Int) {
        viewModelScope.launch {
            val result = repository.toggleLike(workId)
            if (result.isSuccess) {
                loadSocialData()
            } else {
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
