package com.tattoo.studio.domain.usecase

import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.data.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String, newPassword: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Email не может быть пустым"))
        }
        if (code.isBlank()) {
            return Result.failure(AppError.Validation(userMessage = "Код не может быть пустым"))
        }
        if (newPassword.length < 8) {
            return Result.failure(AppError.Validation(userMessage = "Пароль должен быть не менее 8 символов"))
        }
        return authRepository.resetPassword(email, code, newPassword)
    }
}
