package com.tattoo.studio.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.local.prefs.TokenManager
import com.tattoo.studio.data.local.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val tokenManager: TokenManager
) : ViewModel() {

    val userRole: StateFlow<String?> = userPreferences.userRoleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isLoggedIn: StateFlow<Boolean?> = userPreferences.userRoleFlow
        .map { role ->
            val hasToken = tokenManager.getAccessToken() != null
            if (!hasToken) false else role != null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
