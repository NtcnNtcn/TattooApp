package com.tattoo.studio.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.local.db.entity.WorkEntity
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.WorksRepository
import com.tattoo.studio.data.repository.TagsRepository
import com.tattoo.studio.data.repository.SocialRepository
import com.tattoo.studio.data.repository.ApplicationsRepository
import com.tattoo.studio.data.local.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: WorksRepository,
    private val tagsRepository: TagsRepository,
    private val socialRepository: SocialRepository,
    private val applicationsRepository: ApplicationsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val userRole: StateFlow<String?> = userPreferences.userRoleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _works = MutableStateFlow<List<WorkEntity>>(emptyList())
    val works: StateFlow<List<WorkEntity>> = _works.asStateFlow()
    
    private val _tags = MutableStateFlow<List<String>>(listOf("ВСЕ"))
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(setOf("ВСЕ"))
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    private val _bookingSuccess = MutableStateFlow<Boolean?>(null)
    val bookingSuccess: StateFlow<Boolean?> = _bookingSuccess.asStateFlow()

    init {
        loadTags()
        refreshWorks()
    }

    private fun loadTags() {
        viewModelScope.launch {
            val result = tagsRepository.getTags()
            if (result.isSuccess) {
                val fetchedTags = listOf("ВСЕ") + result.getOrNull()?.map { it.name.uppercase() }.orEmpty()
                _tags.value = fetchedTags
            }
        }
    }

    fun toggleTag(tag: String) {
        val current = _selectedTags.value.toMutableSet()
        if (tag == "ВСЕ") {
            current.clear()
            current.add("ВСЕ")
        } else {
            current.remove("ВСЕ")
            if (current.contains(tag)) {
                current.remove(tag)
                if (current.isEmpty()) current.add("ВСЕ")
            } else {
                current.add(tag)
            }
        }
        _selectedTags.value = current
    }

    fun refreshWorks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val activeTags = _selectedTags.value.filter { it != "ВСЕ" }
            val tagFilter = if (activeTags.isEmpty()) null else activeTags.joinToString(",") { it.lowercase() }
            val result = repository.refreshWorks(tags = tagFilter)
            if (result.isSuccess) {
                val entities = result.getOrNull()?.map {
                    WorkEntity(
                        id = it.id,
                        imageUrl = it.imageUrl,
                        status = it.status,
                        likeCount = it.likeCount,
                        createdAt = it.createdAt,
                        tagNames = it.tags.joinToString(",") { tag -> tag.name },
                        isLiked = it.isLiked,
                        isFavorited = it.isFavorited,
                        masterName = it.masterName,
                        masterAvatar = it.masterAvatar,
                        masterId = it.masterId
                    )
                } ?: emptyList()
                _works.value = entities
            } else {
                val throwable = result.exceptionOrNull()
                _error.value = if (throwable is AppError) throwable else throwable?.toAppError()
                    ?: AppError.Unknown()
            }
            _isLoading.value = false
        }
    }

    fun createApplication(masterId: Int, contact: String, message: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = applicationsRepository.createApplication(masterId, contact, message)
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
        _error.value = null
    }

    fun toggleLike(workId: Int) {
        viewModelScope.launch {
            val result = socialRepository.toggleLike(workId)
            if (result.isSuccess) {
                val isActive = result.getOrNull() ?: false
                _works.update { currentWorks ->
                    currentWorks.map { 
                        if (it.id == workId) {
                            it.copy(
                                isLiked = isActive,
                                likeCount = if (isActive) it.likeCount + 1 else it.likeCount - 1
                            )
                        } else it
                    }
                }
            } else {
                _error.value = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
            }
        }
    }

    fun toggleFavorite(workId: Int) {
        viewModelScope.launch {
            val result = socialRepository.toggleFavorite(workId)
            if (result.isSuccess) {
                val isActive = result.getOrNull() ?: false
                _works.update { currentWorks ->
                    currentWorks.map { 
                        if (it.id == workId) it.copy(isFavorited = isActive) else it
                    }
                }
            } else {
                _error.value = result.exceptionOrNull()?.toAppError() ?: AppError.Unknown()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
