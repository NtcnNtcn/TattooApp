package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Email не может быть пустым"))
        }
        return authRepository.forgotPassword(email)
    }
}
