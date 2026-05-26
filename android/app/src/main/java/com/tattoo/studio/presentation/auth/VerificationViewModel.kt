package com.tattoo.studio.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.remote.toAppError
import com.tattoo.studio.data.repository.UserRepository
import com.tattoo.studio.domain.usecase.ResendCodeUseCase
import com.tattoo.studio.domain.usecase.VerifyCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val verifyCodeUseCase: VerifyCodeUseCase,
    private val resendCodeUseCase: ResendCodeUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _awaitingApproval = MutableStateFlow(false)
    val awaitingApproval: StateFlow<Boolean> = _awaitingApproval.asStateFlow()

    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()

    fun verifyCode(email: String, code: String, type: String, onVerified: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            if (type == "email_change") {
                val result = userRepository.verifyEmailChange(email, code)
                if (result.isSuccess) onVerified()
                else _error.value = result.exceptionOrNull()?.toAppError()?.userMessage ?: "Ошибка верификации"
            } else {
                val result = verifyCodeUseCase(email, code, type)
                if (result.isSuccess) {
                    val hasTokens = result.getOrDefault(false)
                    if (hasTokens) onVerified()
                    else _awaitingApproval.value = true
                } else {
                    _error.value = result.exceptionOrNull()?.toAppError()?.userMessage ?: "Ошибка верификации"
                }
            }
            _isLoading.value = false
        }
    }

    fun resendCode(email: String, type: String) {
        if (_resendTimer.value > 0) return
        
        viewModelScope.launch {
            _error.value = null
            val result = resendCodeUseCase(email, type)
            if (result.isSuccess) {
                startTimer()
            } else {
                _error.value = result.exceptionOrNull()?.toAppError()?.userMessage ?: "Ошибка отправки кода"
            }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            _resendTimer.value = 60
            while (_resendTimer.value > 0) {
                kotlinx.coroutines.delay(1000)
                _resendTimer.value -= 1
            }
        }
    }
}
